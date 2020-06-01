package io.orangebuffalo.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.integration.PushNotificationService
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2ClientAuthorizationProvider
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2FailedEvent
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2SucceededEvent
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.services.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.services.storage.StorageAuthorizationRequiredException
import io.orangebuffalo.simpleaccounting.services.storage.StorageProviderResponse
import io.orangebuffalo.simpleaccounting.services.storage.gdrive.impl.DriveFileNotFoundException
import io.orangebuffalo.simpleaccounting.services.storage.gdrive.impl.FolderResponse
import io.orangebuffalo.simpleaccounting.services.storage.gdrive.impl.GoogleDriveApiAdapter
import org.springframework.context.event.EventListener
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

const val OAUTH2_CLIENT_REGISTRATION_ID = "google-drive"
private const val AUTH_EVENT_NAME = "storage.google-drive.auth"

@Service
class GoogleDriveDocumentsStorageService(
    private val userService: PlatformUserService,
    private val repository: GoogleDriveStorageIntegrationRepository,
    private val pushNotificationService: PushNotificationService,
    private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider,
    private val googleDriveApi: GoogleDriveApiAdapter
) : DocumentsStorage {

    override suspend fun saveDocument(request: SaveDocumentRequest): StorageProviderResponse {
        val integration = withDbContext { repository.findByUserId(request.workspace.ownerId) }
            ?: throw StorageAuthorizationRequiredException()

        //todo #81: it can happen that parallel file uploads of the same user create multiple workspace folders: need cleanup
        val workspaceFolder = googleDriveApi.getOrCreateFolder(
            folderName = request.workspace.id?.toString()!!,
            parentFolderId = "${integration.folderId}"
        )

        val newFile = googleDriveApi.uploadFile(
            content = request.content,
            fileName = request.fileName,
            parentFolderId = workspaceFolder
        )

        return StorageProviderResponse(
            storageProviderLocation = newFile.id,
            sizeInBytes = newFile.sizeInBytes
        )
    }

    override fun getId(): String = "google-drive"

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flux<DataBuffer> =
        googleDriveApi.downloadFile(storageLocation)

    private suspend fun buildAuthorizationUrl(): String = clientAuthorizationProvider
        .buildAuthorizationUrl(OAUTH2_CLIENT_REGISTRATION_ID, mapOf("access_type" to "offline"))

    @EventListener
    fun onAuthSuccess(authSucceededEvent: OAuth2SucceededEvent) = authSucceededEvent
        .launchIfClientMatches(OAUTH2_CLIENT_REGISTRATION_ID) {

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
        .launchIfClientMatches(OAUTH2_CLIENT_REGISTRATION_ID) {
            val userId = authFailedEvent.user.id!!
            pushNotificationService.sendPushNotification(
                eventName = AUTH_EVENT_NAME,
                userId = userId,
                // todo #225: send authorization url
                data = GoogleDriveStorageIntegrationStatus(authorizationRequired = true)
            )
        }

    private suspend fun ensureRootFolder(integration: GoogleDriveStorageIntegration): FolderResponse {
        val rooFolder = try {
            integration.folderId?.let { rootFolderId -> googleDriveApi.getFolderById(rootFolderId) }
        } catch (e: DriveFileNotFoundException) {
            null
        }
        return rooFolder ?: googleDriveApi
            .createFolder(folderName = "simple-accounting", parentFolderId = null)
            .also { driveFolder ->
                integration.folderId = driveFolder.id
                repository.save(integration)
            }
    }

    suspend fun getCurrentUserIntegrationStatus(): GoogleDriveStorageIntegrationStatus {
        val currentUser = userService.getCurrentUser()

        val integration = withDbContext { repository.findByUserId(currentUser.id!!) }
            ?: withDbContext {
                repository.save(GoogleDriveStorageIntegration(userId = currentUser.id!!))
            }

        val integrationStatus = GoogleDriveStorageIntegrationStatus(
            folderId = integration.folderId,
            folderName = null,
            authorizationRequired = false
        )

        val rootFolder = try {
            ensureRootFolder(integration)
        } catch (_: StorageAuthorizationRequiredException) {
            return integrationStatus.copy(
                authorizationUrl = buildAuthorizationUrl(),
                authorizationRequired = true
            )
        }

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
