package io.orangebuffalo.simpleaccounting.business.documents.migration

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import org.springframework.stereotype.Service

@Service
class DocumentsMigrationService(
    private val documentsMigrationRepository: DocumentsMigrationRepository,
    private val documentsRepository: DocumentsRepository,
    private val documentsStorages: List<DocumentsStorage>,
    private val documentsMigrationProcessor: DocumentsMigrationProcessor,
    private val platformUsersService: PlatformUsersService,
) {
    suspend fun startDocumentsMigration(): DocumentsMigration {
        val currentUser = platformUsersService.getCurrentUser()
        val userId = currentUser.id!!
        val uploadStorageId = currentUser.documentsStorage ?: throw DocumentsStorageNotConfiguredException()
        val uploadStorage = documentsStorages.firstOrNull { it.getId() == uploadStorageId }
            ?: throw DocumentsUploadStorageNotActiveException()

        if (!uploadStorage.getCurrentUserStorageStatus().active) {
            throw DocumentsUploadStorageNotActiveException()
        }

        if (withDbContext { documentsMigrationRepository.existsByUserIdAndCompletedAtIsNull(userId) }) {
            throw DocumentsMigrationAlreadyInProgressException()
        }

        val storageIdsToMigrateFrom = withDbContext {
            documentsRepository.getStorageStatsByOwner(userId)
                .map { it.storageId }
                .filter { it != uploadStorageId }
        }
        validateDownloadStorages(userId, storageIdsToMigrateFrom)

        val migration = withDbContext {
            val documentsToMigrate = documentsRepository.findIdsByOwnerAndStorageIdNot(
                ownerId = userId,
                storageId = uploadStorageId,
            )
            documentsMigrationRepository.save(
                DocumentsMigration(
                    userId = userId,
                    documentsToMigrate = documentsToMigrate
                        .map { DocumentsMigrationDocument(documentId = it) }
                        .toSet(),
                )
            )
        }

        documentsMigrationProcessor.startMigration(migration.id!!)
        return migration
    }

    private suspend fun validateDownloadStorages(userId: String, storageIds: List<String>) {
        val storagesById = documentsStorages.associateBy { it.getId() }
        val inactiveStorageIds = storageIds
            .filter { storageId -> storagesById[storageId]?.isDownloadAvailableForUser(userId) != true }
            .sorted()

        if (inactiveStorageIds.isNotEmpty()) {
            throw DocumentsMigrationSourceStorageNotActiveException(inactiveStorageIds)
        }
    }
}

class DocumentsStorageNotConfiguredException : RuntimeException("Documents storage is not configured")

class DocumentsUploadStorageNotActiveException : RuntimeException("Documents upload storage is not active")

class DocumentsMigrationSourceStorageNotActiveException(storageIds: List<String>) : RuntimeException(
    "Documents migration source storages are not active: ${storageIds.joinToString()}",
)

class DocumentsMigrationAlreadyInProgressException : RuntimeException("Documents migration is already in progress")
