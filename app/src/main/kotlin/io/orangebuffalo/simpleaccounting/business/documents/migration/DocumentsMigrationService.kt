package io.orangebuffalo.simpleaccounting.business.documents.migration

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import org.springframework.stereotype.Service

@Service
class DocumentsMigrationService(
    private val documentsMigrationRepository: DocumentsMigrationRepository,
    private val documentsRepository: DocumentsRepository,
    private val platformUsersService: PlatformUsersService,
) {
    suspend fun startDocumentsMigration(): DocumentsMigration {
        val currentUser = platformUsersService.getCurrentUser()
        val userId = currentUser.id!!

        return withDbContext {
            val documentsToMigrate = documentsRepository.findIdsByOwnerAndStorageIdNot(
                ownerId = userId,
                storageId = currentUser.documentsStorage,
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
    }
}
