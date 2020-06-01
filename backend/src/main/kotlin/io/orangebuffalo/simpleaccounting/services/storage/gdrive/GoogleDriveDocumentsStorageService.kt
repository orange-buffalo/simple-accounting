package io.orangebuffalo.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.integration.PushNotificationService
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2ClientAuthorizationProvider
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2FailedEvent
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2SucceededEvent
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2WebClientBuilderProvider
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.storage.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

private const val AUTH_EVENT_NAME = "storage.google-drive.auth"
private const val OAUTH2_CLIENT_REGISTRATION_ID = "google-drive"

@Service
class GoogleDriveDocumentsStorageService(
    private val userService: PlatformUserService,
    private val repository: GoogleDriveStorageIntegrationRepository,
    private val pushNotificationService: PushNotificationService,
    private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider,
    private val webClientBuilderProvider: OAuth2WebClientBuilderProvider,
    @Value("\${simpleaccounting.documents.storage.google-drive.base-api-url}") private val baseApiUrl: String
) : DocumentsStorage {

    override suspend fun saveDocument(request: SaveDocumentRequest): StorageProviderResponse {
        val integration = withDbContext { repository.findByUserId(request.workspace.ownerId) }
            ?: throw StorageAuthorizationRequiredException()

        val workspaceFolder = getOrCreateWorkspaceFolder(integration, request.workspace)

        val fileMetadata = GDriveCreateFileRequest(
            name = request.fileName,
            parents = listOf(workspaceFolder.id!!),
            mimeType = ""
        )

        val newFile = uploadFileToDrive(request, fileMetadata)

        return StorageProviderResponse(
            newFile.id!!,
            newFile.size
        )
    }

    private suspend fun uploadFileToDrive(
        request: SaveDocumentRequest,
        fileMetadata: GDriveCreateFileRequest
    ): GDriveFile = createWebClient()
        .post()
        .uri { builder ->
            builder.path("upload/drive/v3/files")
                .queryParam("fields", "id, size")
                .queryParam("uploadType", "multipart")
                .build()
        }
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(
            BodyInserters.fromMultipartData(
                MultipartBodyBuilder()
                    .apply {
                        part("metadata", fileMetadata, MediaType.APPLICATION_JSON)
                        asyncPart("media", request.content, DataBuffer::class.java)
                    }
                    .build()
            )
        )
        .accept(MediaType.APPLICATION_JSON)
        .executeDriveRequest { errorJson ->
            "Error while uploading $fileMetadata: $errorJson"
        }
        .bodyToMono(GDriveFile::class.java)
        .awaitFirst()

    //todo #81: it can happen that parallel file uploads of the same user create multiple workspace folders: need cleanup
    private suspend fun getOrCreateWorkspaceFolder(
        integration: GoogleDriveStorageIntegration,
        workspace: Workspace
    ): GDriveFile {
        val workspaceFolders = createWebClient()
            .get()
            .uri { builder ->
                builder.path("/drive/v3/files")
                    .queryParam(
                        "q",
                        "'${integration.folderId}' in parents and name = '${workspace.id}' and trashed = false"
                    )
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .executeDriveRequest { errorJson ->
                "Error while retrieving workspace folder for ${integration.id}: $errorJson"
            }
            .bodyToMono(GDriveFiles::class.java)
            .awaitFirst()

        return if (workspaceFolders.files.isEmpty()) {
            createWebClient()
                .post()
                .uri { builder ->
                    builder.path("/drive/v3/files")
                        .queryParam("fields", "id")
                        .build()
                }
                .bodyValue(
                    GDriveCreateFileRequest(
                        name = "${workspace.id}",
                        mimeType = "application/vnd.google-apps.folder",
                        parents = listOf(integration.folderId!!)
                    )
                )
                .accept(MediaType.APPLICATION_JSON)
                .executeDriveRequest { errorJson ->
                    "Error while creating workspace folder for ${workspace.id}: $errorJson"
                }
                .bodyToMono(GDriveFile::class.java)
                .awaitFirst()
        } else {
            workspaceFolders.files[0]
        }
    }

    override fun getId(): String = "google-drive"

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flux<DataBuffer> {
        return createWebClient()
            .get()
            .uri { builder ->
                builder.path("/drive/v3/files/$storageLocation")
                    .queryParam("alt", "media")
                    .build()
            }
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .executeDriveRequest { errorJson ->
                "Error while downloading $storageLocation: $errorJson"
            }
            .body(BodyExtractors.toDataBuffers())
    }

    private suspend fun buildAuthorizationUrl(): String = clientAuthorizationProvider
        .buildAuthorizationUrl(OAUTH2_CLIENT_REGISTRATION_ID, mapOf("access_type" to "offline"))

    private suspend fun getRootFolder(integration: GoogleDriveStorageIntegration): GDriveFile? =
        integration.folderId?.let {
            createWebClient()
                .get()
                .uri { builder ->
                    builder.path("/drive/v3/files/${integration.folderId}")
                        .queryParam("fields", "name, trashed, id")
                        .build()
                }
                .accept(MediaType.APPLICATION_JSON)
                .executeDriveRequest { errorJson ->
                    "Error while retrieving root folder for ${integration.id}: $errorJson"
                }
                .bodyToMono(GDriveFile::class.java)
                .awaitFirst()
                .let { rootFolder -> if (rootFolder.trashed!!) null else rootFolder }
        }

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

    private suspend fun ensureRootFolder(integration: GoogleDriveStorageIntegration): GDriveFile {
        val rooFolder = try {
            getRootFolder(integration)
        } catch (e: DriveFileNotFoundException) {
            null
        }

        return rooFolder ?: createWebClient()
            .post()
            .uri { builder ->
                builder.path("/drive/v3/files")
                    .queryParam("fields", "id, name")
                    .build()
            }
            .bodyValue(
                GDriveCreateFileRequest(
                    name = "simple-accounting",
                    mimeType = "application/vnd.google-apps.folder",
                    parents = emptyList()
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .executeDriveRequest { errorJson -> errorJson ?: "Invalid request" }
            .bodyToMono(GDriveFile::class.java)
            .awaitFirst()
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

    private fun createWebClient() = webClientBuilderProvider
        .forClient(OAUTH2_CLIENT_REGISTRATION_ID)
        .baseUrl(baseApiUrl)
        .build()

    private suspend inline fun WebClient.RequestHeadersSpec<*>.executeDriveRequest(
        errorDescriptor: (errorJson: String?) -> String
    ): ClientResponse {
        val clientResponse = try {
            this.exchange().awaitFirst()
        } catch (e: OAuth2AuthorizationException) {
            throw StorageAuthorizationRequiredException()
        }

        val statusCode = clientResponse.statusCode()
        if (statusCode == HttpStatus.UNAUTHORIZED || statusCode == HttpStatus.FORBIDDEN) {
            throw StorageAuthorizationRequiredException()
        } else if (statusCode != HttpStatus.OK) {
            val errorJson = clientResponse.bodyToMono(String::class.java).awaitFirstOrNull()
            if (statusCode == HttpStatus.NOT_FOUND) {
                throw DriveFileNotFoundException(errorJson)
            } else {
                throw DocumentStorageException(errorDescriptor(errorJson))
            }
        }

        return clientResponse
    }
}

data class GoogleDriveStorageIntegrationStatus(
    val folderId: String? = null,
    val folderName: String? = null,
    val authorizationUrl: String? = null,
    val authorizationRequired: Boolean
)

private data class GDriveFiles(
    val files: List<GDriveFile>
)

private data class GDriveFile(
    val id: String? = null,
    val size: Long? = null,
    val name: String? = null,
    val trashed: Boolean? = null
)

private data class GDriveCreateFileRequest(
    val name: String,
    val mimeType: String,
    val parents: List<String>
)

private class DriveFileNotFoundException(message: String?) : DocumentStorageException(message)
