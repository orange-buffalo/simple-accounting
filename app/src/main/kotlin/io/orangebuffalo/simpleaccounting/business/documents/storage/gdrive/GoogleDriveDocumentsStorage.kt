package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive

import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.integration.pushnotifications.PushNotificationService
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2ClientAuthorizationProvider
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2FailedEvent
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2SucceededEvent
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.documents.storage.*
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.impl.DriveFileNotFoundException
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.impl.FolderResponse
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.impl.GoogleDriveApiAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.context.event.EventListener
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service

const val OAUTH2_CLIENT_REGISTRATION_ID = "google-drive"
const val AUTH_EVENT_NAME = "storage.google-drive.auth"

private val log = mu.KotlinLogging.logger {}

@Service
class GoogleDriveDocumentsStorage(
    private val userService: PlatformUsersService,
    private val repository: GoogleDriveStorageIntegrationRepository,
    private val pushNotificationService: PushNotificationService,
    private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider,
    private val googleDriveApi: GoogleDriveApiAdapter
) : DocumentsStorage {

    private val workspaceFolderMutex = Mutex()

    override suspend fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse {
        val integration = withDbContext { repository.findByUserId(request.workspace.ownerId) }
            ?: throw StorageAuthorizationRequiredException()

        val workspaceFolder = getOrCreateWorkspaceFolder(request, integration)

        val newFile = googleDriveApi.uploadFile(
            content = request.content,
            fileName = request.fileName,
            parentFolderId = workspaceFolder
        )

        return SaveDocumentResponse(
            storageLocation = newFile.id,
            sizeInBytes = newFile.sizeInBytes
        )
    }

    private suspend fun getOrCreateWorkspaceFolder(
        request: SaveDocumentRequest,
        integration: GoogleDriveStorageIntegration
    ): String {
        val workspaceFolderName = request.workspace.id!!.toString()

        val workspaceFolder = googleDriveApi.findFolderByNameAndParent(
            folderName = workspaceFolderName,
            parentFolderId = "${integration.folderId}"
        )
        if (workspaceFolder != null) return workspaceFolder

        // we consider a global lock acceptable here as creation of new workspace folder is a rare operation
        // the lock prevents multiple folders to be created for the same workspace in case of parallel upload requests
        return workspaceFolderMutex.withLock {
            googleDriveApi.createFolder(workspaceFolderName, integration.folderId).id
        }
    }

    override fun getId(): String = "google-drive"

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flow<DataBuffer> =
        googleDriveApi.downloadFile(storageLocation)

    override suspend fun getCurrentUserStorageStatus(): DocumentsStorageStatus {
        val integrationStatus = getCurrentUserIntegrationStatus()
        return DocumentsStorageStatus(
            active = !integrationStatus.authorizationRequired
        )
    }

    private suspend fun buildAuthorizationUrl(): String = clientAuthorizationProvider
        .buildAuthorizationUrl(OAUTH2_CLIENT_REGISTRATION_ID, mapOf("access_type" to "offline"))

    @EventListener
    fun onAuthSuccess(authSucceededEvent: OAuth2SucceededEvent) = authSucceededEvent
        .executeInSourceContext(OAUTH2_CLIENT_REGISTRATION_ID) {

            val user = authSucceededEvent.user
            val integration = withDbContext {
                repository.findByUserId(user.id!!)
                    ?: GoogleDriveStorageIntegration(userId = user.id!!)
            }

            val rootFolder = ensureRootFolder(integration)

            withDbContext {
                repository.save(integration)
            }

            pushNotificationService.sendPushNotification(
                eventName = AUTH_EVENT_NAME,
                userId = integration.userId,
                data = GoogleDriveStorageIntegrationStatus(
                    folderId = rootFolder.id,
                    folderName = rootFolder.name,
                    authorizationRequired = false
                )
            )
        }

    @EventListener
    fun onAuthFailure(authFailedEvent: OAuth2FailedEvent) = authFailedEvent
        .executeInSourceContext(OAUTH2_CLIENT_REGISTRATION_ID) {
            pushNotificationService.sendPushNotification(
                eventName = AUTH_EVENT_NAME,
                userId = authFailedEvent.user.id!!,
                data = GoogleDriveStorageIntegrationStatus(
                    authorizationRequired = true,
                    authorizationUrl = buildAuthorizationUrl()
                )
            )
        }

    private suspend fun ensureRootFolder(integration: GoogleDriveStorageIntegration): FolderResponse {
        log.debug { "Ensuring root folder for Google Drive integration $integration" }

        val rootFolder = try {
            integration.folderId?.let { rootFolderId -> googleDriveApi.getFolderById(rootFolderId) }
        } catch (e: DriveFileNotFoundException) {
            log.debug { "Root folder not found: ${e.message}" }
            null
        }
        return rootFolder ?: googleDriveApi
            .createFolder(folderName = "simple-accounting", parentFolderId = null)
            .also { driveFolder ->
                integration.folderId = driveFolder.id
                repository.save(integration)
                log.debug { "Root folder created $driveFolder and saved to integration $integration" }
            }
    }

    suspend fun getCurrentUserIntegrationStatus(): GoogleDriveStorageIntegrationStatus {
        val currentUser = userService.getCurrentUser()
        log.debug { "Getting Google Drive integration status for user ${currentUser.id}" }

        val integration = withDbContext {
            repository.findByUserId(currentUser.id!!)
                ?: repository.save(GoogleDriveStorageIntegration(userId = currentUser.id!!))
        }

        val integrationStatus = GoogleDriveStorageIntegrationStatus(
            folderId = integration.folderId,
            folderName = null,
            authorizationRequired = false
        )

        val rootFolder = try {
            ensureRootFolder(integration)
        } catch (_: StorageAuthorizationRequiredException) {
            log.debug { "Authorization required for Google Drive integration" }
            return integrationStatus.copy(
                authorizationUrl = buildAuthorizationUrl(),
                authorizationRequired = true
            )
        }

        log.debug { "Google Drive integration status for user ${currentUser.id} is $integrationStatus" }

        return integrationStatus.copy(
            folderName = rootFolder.name,
            folderId = rootFolder.id
        )
    }
}

data class GoogleDriveStorageIntegrationStatus(
    val folderId: String? = null,
    val folderName: String? = null,
    val authorizationUrl: String? = null,
    val authorizationRequired: Boolean
)
