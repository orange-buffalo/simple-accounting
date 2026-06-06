package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.impl

import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentStorageException
import io.orangebuffalo.simpleaccounting.business.documents.storage.StorageAuthorizationRequiredException
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveDocumentsStorageProperties
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.OAUTH2_CLIENT_REGISTRATION_ID
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2WebClientBuilderProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters.fromMultipartData
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import reactor.core.publisher.Flux

private val log = mu.KotlinLogging.logger {}

@Component
class GoogleDriveApiAdapter(
    private val webClientBuilderProvider: OAuth2WebClientBuilderProvider,
    private val googleDriveDocumentsStorageProperties: GoogleDriveDocumentsStorageProperties,
) {

    suspend fun uploadFile(
        content: Flux<DataBuffer>,
        fileName: String,
        parentFolderId: String
    ): UploadFileResponse {
        val fileMetadata = GDriveCreateFileRequest(
            name = fileName,
            parents = listOf(parentFolderId),
            mimeType = ""
        )

        return createWebClient()
            .post()
            .uri { builder ->
                builder.path("upload/drive/v3/files")
                    .queryParam("fields", "id, size")
                    .queryParam("uploadType", "multipart")
                    .build()
            }
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(
                fromMultipartData(
                    MultipartBodyBuilder()
                        .apply {
                            part("metadata", fileMetadata, MediaType.APPLICATION_JSON)
                            asyncPart("media", content, DataBuffer::class.java)
                        }
                        .build()
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .executeDriveRequest(
                errorDescriptor = { errorJson ->
                    "Error while uploading $fileMetadata: $errorJson"
                },
                responseHandler = { clientResponse ->
                    clientResponse.bodyToMono(GDriveFile::class.java)
                        .map { driveFile -> driveFile.toUploadFileResponse() }
                        .awaitSingle()
                }
            )
    }

    suspend fun downloadFile(fileId: String): Flow<DataBuffer> {
        return createWebClient()
            .get()
            .uri { builder ->
                builder.path("/drive/v3/files/$fileId")
                    .queryParam("alt", "media")
                    .build()
            }
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .executeDriveRequest(
                errorDescriptor = { errorJson ->
                    "Error while downloading $fileId: $errorJson"
                },
                responseHandler = { clientResponse ->
                    clientResponse.body(BodyExtractors.toDataBuffers()).asFlow()
                }
            )
    }

    suspend fun deleteFile(fileId: String) {
        createWebClient()
            .delete()
            .uri { builder ->
                builder.path("/drive/v3/files/$fileId")
                    .build()
            }
            .executeDriveRequest(
                successStatuses = setOf(HttpStatus.OK, HttpStatus.NO_CONTENT),
                errorDescriptor = { errorJson ->
                    "Error while deleting $fileId: $errorJson"
                },
                responseHandler = {}
            )
    }

    suspend fun findFolderByNameAndParent(
        folderName: String,
        parentFolderId: String
    ): String? {
        val matchingFolders = createWebClient()
            .get()
            .uri { builder ->
                builder.path("/drive/v3/files")
                    .queryParam(
                        "q",
                        "'$parentFolderId' in parents and name = '$folderName' and trashed = false"
                    )
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .executeDriveRequest(
                errorDescriptor = { errorJson ->
                    "Error while retrieving folder $folderName for $parentFolderId: $errorJson"
                },
                responseHandler = { clientResponse ->
                    clientResponse.bodyToMono(GDriveFiles::class.java).awaitSingle()
                }
            )

        return if (matchingFolders.files.isEmpty()) null else matchingFolders.files[0].id
    }

    suspend fun createFolder(
        folderName: String,
        parentFolderId: String?
    ): FolderResponse {
        log.debug { "Creating folder $folderName under $parentFolderId" }
        return createWebClient()
            .post()
            .uri { builder ->
                builder.path("/drive/v3/files")
                    .queryParam("fields", "id, name")
                    .build()
            }
            .bodyValue(
                GDriveCreateFileRequest(
                    name = folderName,
                    mimeType = "application/vnd.google-apps.folder",
                    parents = if (parentFolderId == null) emptyList() else listOf(parentFolderId)
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .executeDriveRequest(
                errorDescriptor = { errorJson ->
                    log.debug { "Error while creating folder $folderName: $errorJson" }
                    "Error while creating folder $folderName: $errorJson"
                },
                responseHandler = { clientResponse ->
                    clientResponse.bodyToMono(GDriveFile::class.java)
                        .map { driveFile -> driveFile.toFolderResponse() }
                        .awaitSingle()
                }
            )
            .also {
                log.debug { "Folder $folderName created: $it" }
            }
    }

    suspend fun getFolderById(folderId: String): FolderResponse? {
        log.debug { "Retrieving folder by id $folderId" }
        return createWebClient()
            .get()
            .uri { builder ->
                builder.path("/drive/v3/files/$folderId")
                    .queryParam("fields", "name, trashed, id")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .executeNullableDriveRequest(
                errorDescriptor = { errorJson ->
                    log.debug { "Error while retrieving folder $folderId: $errorJson" }
                    "Error while retrieving folder $folderId: $errorJson"
                },
                responseHandler = { clientResponse ->
                    clientResponse.bodyToMono(GDriveFile::class.java)
                        .filter { driveFile -> !driveFile.trashed!! }
                        .map { driveFile -> driveFile.toFolderResponse() }
                        .awaitFirstOrNull()
                }
            )
            .also {
                log.debug { "Folder $folderId retrieved: $it" }
            }
    }

    private fun createWebClient() = webClientBuilderProvider
        .forClient(OAUTH2_CLIENT_REGISTRATION_ID)
        .baseUrl(googleDriveDocumentsStorageProperties.baseApiUrl)
        .build()

    private suspend inline fun <T : Any> WebClient.RequestHeadersSpec<out WebClient.RequestHeadersSpec<*>>.executeDriveRequest(
        successStatuses: Set<HttpStatus> = setOf(HttpStatus.OK),
        crossinline errorDescriptor: (errorJson: String?) -> String,
        crossinline responseHandler: suspend (ClientResponse) -> T,
    ): T {
        log.debug { "Executing request: $this" }
        return try {
            this.awaitExchange<T> { clientResponse ->
                val statusCode = clientResponse.statusCode()
                if (statusCode == HttpStatus.UNAUTHORIZED || statusCode == HttpStatus.FORBIDDEN) {
                    log.debug { "Authorization required: $statusCode" }
                    throw StorageAuthorizationRequiredException(message = "Not authorized: $statusCode")
                } else if (statusCode !in successStatuses) {
                    val errorJson = clientResponse.bodyToMono(String::class.java).awaitFirstOrNull()
                    log.debug { "Error response with code $statusCode: $errorJson" }
                    if (statusCode == HttpStatus.NOT_FOUND) {
                        throw DriveFileNotFoundException(errorJson)
                    } else {
                        throw DocumentStorageException(errorDescriptor(errorJson))
                    }
                }
                log.debug { "Request executed successfully: $statusCode" }

                responseHandler(clientResponse)
            }
        } catch (e: OAuth2AuthorizationException) {
            log.debug { "Authorization error: ${e.message}" }
            throw StorageAuthorizationRequiredException(cause = e)
        }
    }

    private suspend inline fun <T : Any> WebClient.RequestHeadersSpec<out WebClient.RequestHeadersSpec<*>>.executeNullableDriveRequest(
        successStatuses: Set<HttpStatus> = setOf(HttpStatus.OK),
        crossinline errorDescriptor: (errorJson: String?) -> String,
        crossinline responseHandler: suspend (ClientResponse) -> T?,
    ): T? {
        log.debug { "Executing request: $this" }
        return try {
            this.awaitExchangeOrNull<T> { clientResponse ->
                val statusCode = clientResponse.statusCode()
                if (statusCode == HttpStatus.UNAUTHORIZED || statusCode == HttpStatus.FORBIDDEN) {
                    log.debug { "Authorization required: $statusCode" }
                    throw StorageAuthorizationRequiredException(message = "Not authorized: $statusCode")
                } else if (statusCode !in successStatuses) {
                    val errorJson = clientResponse.bodyToMono(String::class.java).awaitFirstOrNull()
                    log.debug { "Error response with code $statusCode: $errorJson" }
                    if (statusCode == HttpStatus.NOT_FOUND) {
                        throw DriveFileNotFoundException(errorJson)
                    } else {
                        throw DocumentStorageException(errorDescriptor(errorJson))
                    }
                }
                log.debug { "Request executed successfully: $statusCode" }

                responseHandler(clientResponse)
            }
        } catch (e: OAuth2AuthorizationException) {
            log.debug { "Authorization error: ${e.message}" }
            throw StorageAuthorizationRequiredException(cause = e)
        }
    }
}

data class UploadFileResponse(
    val id: String,
    val sizeInBytes: Long?
)

data class FolderResponse(
    val id: String,
    val name: String
)

class DriveFileNotFoundException(message: String?) : DocumentStorageException(message)

private data class GDriveFiles(
    val files: List<GDriveFile>
)

private data class GDriveFile(
    val id: String? = null,
    val size: Long? = null,
    val name: String? = null,
    val trashed: Boolean? = null
) {
    fun toUploadFileResponse() = UploadFileResponse(id = id!!, sizeInBytes = size)
    fun toFolderResponse() = FolderResponse(id = id!!, name = name!!)
}

private data class GDriveCreateFileRequest(
    val name: String,
    val mimeType: String,
    val parents: List<String>
)
