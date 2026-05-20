package io.orangebuffalo.simpleaccounting.business.documents.migration

import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.asFlux
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import kotlin.coroutines.ContinuationInterceptor

private val log = KotlinLogging.logger {}

@Service
class DocumentsMigrationProcessor(
    private val documentsMigrationRepository: DocumentsMigrationRepository,
    private val documentsRepository: DocumentsRepository,
    private val documentsStorages: List<DocumentsStorage>,
    private val platformUsersService: PlatformUsersService,
    private val workspacesService: WorkspacesService,
    private val timeService: TimeService,
    transactionManager: PlatformTransactionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val migrationTransactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    suspend fun startMigration(migrationId: String) {
        val sourceContext = currentCoroutineContext()
            .minusKey(Job)
            .minusKey(ContinuationInterceptor)
        scope.launch(sourceContext) {
            processMigration(migrationId)
        }
    }

    suspend fun processMigration(migrationId: String) {
        val migration = withDbContext { documentsMigrationRepository.findByIdOrNull(migrationId) } ?: return
        val user = platformUsersService.getUserByUserId(migration.userId)

        runAs(user.toSecurityPrincipal()) {
            try {
                migration.documentsToMigrate
                    .map { it.documentId }
                    .sorted()
                    .forEach { documentId ->
                        scope.ensureActive()
                        processDocument(migration, documentId)
                    }
                completeMigration(migrationId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error(e) { "Unexpected failure while processing documents migration $migrationId" }
                completeMigration(migrationId)
            }
        }
    }

    private suspend fun processDocument(migration: DocumentsMigration, documentId: String) {
        var documentProcessingFinished = false
        try {
            migrateDocument(migration, documentId)
            documentProcessingFinished = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "Failed to migrate document $documentId in migration ${migration.id}" }
            documentProcessingFinished = true
        } finally {
            if (documentProcessingFinished) {
                incrementMigratedDocumentsCount(migration.id!!)
            }
        }
    }

    private suspend fun migrateDocument(migration: DocumentsMigration, documentId: String) {
        val document = withDbContext { documentsRepository.findByIdOrNull(documentId) }
            ?: throw IllegalStateException("Document $documentId is not found")
        val sourceStorageLocation = document.storageLocation
            ?: throw IllegalStateException("Document $documentId has no storage location")
        val uploadStorageId = platformUsersService.getUserByUserId(migration.userId).documentsStorage
            ?: throw IllegalStateException("Documents storage is not configured for user ${migration.userId}")
        val uploadStorage = getStorage(uploadStorageId)
        val sourceStorage = getStorage(document.storageId)
        val workspace = workspacesService.getWorkspace(document.workspaceId)

        val saveResponse = uploadStorage.saveDocument(
            SaveDocumentRequest(
                fileName = document.name,
                content = sourceStorage.getDocumentContent(workspace, sourceStorageLocation).asFlux(),
                workspace = workspace,
                contentType = document.mimeType,
            )
        )

        updateDocumentStorage(
            document = document,
            storageId = uploadStorage.getId(),
            storageLocation = saveResponse.storageLocation,
            sizeInBytes = saveResponse.sizeInBytes,
        )

        sourceStorage.deleteDocument(workspace, sourceStorageLocation)
    }

    private fun getStorage(storageId: String) = documentsStorages
        .firstOrNull { it.getId() == storageId }
        ?: throw IllegalStateException("Documents storage $storageId is not configured")

    private suspend fun updateDocumentStorage(
        document: Document,
        storageId: String,
        storageLocation: String,
        sizeInBytes: Long?,
    ) = executeInMigrationTransaction {
        documentsRepository.save(
            document.copy(
                storageId = storageId,
                storageLocation = storageLocation,
                sizeInBytes = sizeInBytes,
            )
        )
    }

    private suspend fun incrementMigratedDocumentsCount(migrationId: String) = executeInMigrationTransaction {
        val migration = documentsMigrationRepository.findById(migrationId)
            .orElseThrow { IllegalStateException("Documents migration $migrationId is not found") }
        documentsMigrationRepository.save(
            migration.copy(migratedDocumentsCount = migration.migratedDocumentsCount + 1)
        )
    }

    private suspend fun completeMigration(migrationId: String) = executeInMigrationTransaction {
        val migration = documentsMigrationRepository.findById(migrationId)
            .orElseThrow { IllegalStateException("Documents migration $migrationId is not found") }
        documentsMigrationRepository.save(migration.copy(completedAt = timeService.currentTime()))
    }

    private suspend fun <T> executeInMigrationTransaction(block: () -> T): T = withDbContext {
        @Suppress("UNCHECKED_CAST")
        migrationTransactionTemplate.execute { block() } as T
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }
}
