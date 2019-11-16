package io.orangebuffalo.accounting.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.integration.PushNotificationService
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.oauth2.AuthFailedEvent
import io.orangebuffalo.accounting.simpleaccounting.services.oauth2.AuthSucceededEvent
import io.orangebuffalo.accounting.simpleaccounting.services.oauth2.OAuth2Service
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.security.ensureRegularUserPrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.storage.DocumentsStorage
import io.orangebuffalo.accounting.simpleaccounting.services.storage.StorageAuthorizationRequiredException
import io.orangebuffalo.accounting.simpleaccounting.services.storage.StorageProviderResponse
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.context.event.EventListener
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.util.function.Consumer

private const val AUTH_EVENT_NAME = "storage.google-drive.auth"
private const val OAUTH2_CLIENT_REGISTRATION_ID = "google-drive"

@Service
class GoogleDriveDocumentsStorageService(
    private val userService: PlatformUserService,
    private val repository: GoogleDriveStorageIntegrationRepository,
    private val pushNotificationService: PushNotificationService,
    private val oauthService: OAuth2Service
) : DocumentsStorage {

    override suspend fun saveDocument(file: FilePart, workspace: Workspace): StorageProviderResponse {
        val integration = withDbContext { repository.findByUser(workspace.owner) }
            ?: throw StorageAuthorizationRequiredException()

        val authorizedClient = getOAuth2AuthorizedClient(workspace.owner.userName)
            ?: throw StorageAuthorizationRequiredException()

        val workspaceFolder = getOrCreateWorkspaceFolder(integration, workspace, authorizedClient)

        val fileMetadata = GDriveCreateFileRequest(
            name = file.filename(),
            parents = listOf(workspaceFolder.id!!),
            mimeType = ""
        )

        val newFile = uploadFileToDrive(file, fileMetadata, authorizedClient, workspace.owner.userName)

        return StorageProviderResponse(
            newFile.id!!,
            newFile.size
        )
    }

    private suspend fun uploadFileToDrive(
        file: FilePart,
        fileMetadata: GDriveCreateFileRequest,
        authorizedClient: OAuth2AuthorizedClient,
        userName: String
    ): GDriveFile = createWebClient()
        .post()
        .uri { builder ->
            builder.path("upload/drive/v3/files")
                .queryParam("fields", "id,size")
                .queryParam("uploadType", "multipart")
                .build()
        }
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(
            BodyInserters.fromMultipartData(
                MultipartBodyBuilder()
                    .apply {
                        part("metadata", fileMetadata, MediaType.APPLICATION_JSON)
                        asyncPart("media", file.content(), DataBuffer::class.java)
                    }
                    .build()
            )
        )
        .accept(MediaType.APPLICATION_JSON)
        .exchangeAuthorized(authorizedClient, userName) { errorJson ->
            "Error while uploading $fileMetadata: $errorJson"
        }
        .bodyToMono(GDriveFile::class.java)
        .awaitFirst()

    //todo #81: it can happen that parallel file uploads of the same user create multiple workspace folders: need cleanup
    private suspend fun getOrCreateWorkspaceFolder(
        integration: GoogleDriveStorageIntegration,
        workspace: Workspace,
        authorizedClient: OAuth2AuthorizedClient
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
            .exchangeAuthorized(authorizedClient, workspace.owner.userName) { errorJson ->
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
                .exchangeAuthorized(authorizedClient, workspace.owner.userName) { errorJson ->
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
            .exchangeAuthorized(userName = workspace.owner.userName) { errorJson ->
                "Error while downloading $storageLocation: $errorJson"
            }
            .body(BodyExtractors.toDataBuffers())
    }

    private suspend fun buildAuthorizationUrl(): String =
        oauthService.buildAuthorizationUrl(OAUTH2_CLIENT_REGISTRATION_ID)

    private suspend fun getRootFolder(
        integration: GoogleDriveStorageIntegration,
        authorizedClient: OAuth2AuthorizedClient? = null
    ): GDriveFile? {
        return integration.folderId?.let {
            createWebClient()
                .get()
                .uri { builder ->
                    builder.path("/drive/v3/files/${integration.folderId}")
                        .queryParam("fields", "name, trashed, id")
                        .build()
                }
                .accept(MediaType.APPLICATION_JSON)
                .exchangeAuthorized(authorizedClient, integration.user.userName) { errorJson ->
                    "Error while retrieving root folder for ${integration.id}: $errorJson"
                }
                .bodyToMono(GDriveFile::class.java)
                .awaitFirst()
                .let { rootFolder -> if (rootFolder.trashed!!) null else rootFolder }
        }
    }

    @EventListener
    fun onAuthSuccess(authSucceededEvent: AuthSucceededEvent) = authSucceededEvent
        .launchIfClientMatches(OAUTH2_CLIENT_REGISTRATION_ID) {

            val user = authSucceededEvent.user
            val integration = withDbContext {
                repository.findByUser(user)
                    ?: GoogleDriveStorageIntegration(user = user)
            }

            val authorizedClient = oauthService.getOAuth2AuthorizedClient(OAUTH2_CLIENT_REGISTRATION_ID, user.userName)

            val rootFolder = ensureRootFolder(integration, authorizedClient)

            withDbContext {
                repository.save(integration)
            }

            pushNotificationService.sendPushNotification(
                eventName = AUTH_EVENT_NAME,
                user = integration.user,
                data = GoogleDriveStorageIntegrationStatus(
                    folderId = rootFolder.id,
                    folderName = rootFolder.name
                )
            )
        }

    @EventListener
    fun onAuthFailure(authFailedEvent: AuthFailedEvent) = authFailedEvent
        .launchIfClientMatches(OAUTH2_CLIENT_REGISTRATION_ID) {
            val user = authFailedEvent.user
            pushNotificationService.sendPushNotification(
                eventName = AUTH_EVENT_NAME,
                user = user,
                data = GoogleDriveStorageIntegrationStatus()
            )
        }

    private suspend fun ensureRootFolder(
        integration: GoogleDriveStorageIntegration,
        authorizedClient: OAuth2AuthorizedClient? = null
    ): GDriveFile {
        val rooFolder = try {
            getRootFolder(integration, authorizedClient)
        } catch (e: DriveFileNotFoundException) {
            null
        }

        return rooFolder ?: createWebClient()
            .post()
            .uri { builder ->
                builder.path("/drive/v3/files")
                    .queryParam("fields", "id,name")
                    .build()
            }
            .bodyValue(
                GDriveCreateFileRequest(
                    name = "simple-accounting",
                    mimeType = "application/vnd.google-apps.folder",
                    parents = emptyList()
                )
            )
            .attributes(setupDriveAuthorization(authorizedClient, integration.user.userName))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .flatMap { it.bodyToMono(GDriveFile::class.java) }
            .awaitFirst()
            .also { driveFolder ->
                integration.folderId = driveFolder.id
                repository.save(integration)
            }
    }

    suspend fun getCurrentUserIntegrationStatus(): GoogleDriveStorageIntegrationStatus  {
        val authorizedClient = getOAuth2AuthorizedClient(ensureRegularUserPrincipal().userName)
            ?: return GoogleDriveStorageIntegrationStatus(authorizationUrl = buildAuthorizationUrl())

        val currentUser = userService.getCurrentUser()
        val integration = withDbContext { repository.findByUser(currentUser) }
            ?: withDbContext {
                repository.save(GoogleDriveStorageIntegration(user = currentUser))
            }

        val rootFolder = try {
            ensureRootFolder(integration, authorizedClient)
        } catch (e: StorageAuthorizationRequiredException) {
            return GoogleDriveStorageIntegrationStatus(authorizationUrl = buildAuthorizationUrl())
        }

        return GoogleDriveStorageIntegrationStatus(
            folderName = rootFolder.name,
            folderId = rootFolder.id
        )
    }

    private fun createWebClient() = oauthService.createWebClient("https://www.googleapis.com")

    private suspend inline fun WebClient.RequestHeadersSpec<*>.exchangeAuthorized(
        authorizedClient: OAuth2AuthorizedClient? = null,
        userName: String,
        errorDescriptor: (errorJson: String?) -> String
    ): ClientResponse {
        val clientResponse = this.attributes(setupDriveAuthorization(authorizedClient, userName))
            .exchange()
            .awaitFirst()

        if (clientResponse.statusCode() == HttpStatus.UNAUTHORIZED) {
            oauthService.deleteAuthorizedClient(OAUTH2_CLIENT_REGISTRATION_ID)
            throw StorageAuthorizationRequiredException()
        } else if (clientResponse.statusCode() != HttpStatus.OK) {
            val errorJson = clientResponse.bodyToMono(String::class.java).awaitFirstOrNull()
            if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                throw DriveFileNotFoundException(errorJson)
            } else {
                throw IllegalStateException(errorDescriptor(errorJson))
            }
        }

        return clientResponse
    }

    private suspend fun getOAuth2AuthorizedClient(userName: String): OAuth2AuthorizedClient? {
        return oauthService.getOAuth2AuthorizedClient(OAUTH2_CLIENT_REGISTRATION_ID, userName)
    }

    private suspend fun setupDriveAuthorization(
        authorizedClient: OAuth2AuthorizedClient? = null,
        userName: String
    ): Consumer<Map<String, Any>> {

        val client = authorizedClient
            ?: getOAuth2AuthorizedClient(userName)
            ?: throw StorageAuthorizationRequiredException()

        return ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(client)
    }
}

data class GoogleDriveStorageIntegrationStatus(
    val folderId: String? = null,
    val folderName: String? = null,
    val authorizationUrl: String? = null
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

private class DriveFileNotFoundException(message: String?) : RuntimeException(message)
