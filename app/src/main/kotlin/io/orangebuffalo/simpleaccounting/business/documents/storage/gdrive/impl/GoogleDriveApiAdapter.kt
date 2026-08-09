package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.impl

import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentStorageException
import io.orangebuffalo.simpleaccounting.business.documents.storage.StorageAuthorizationRequiredException
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveDocumentsStorageProperties
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.OAUTH2_CLIENT_REGISTRATION_ID
import io.orangebuffalo.simpleaccounting.infra.InputStreamProvider
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2RestClientBuilderProvider
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

private val log = mu.KotlinLogging.logger {}

@Component
class GoogleDriveApiAdapter(
    private val restClientBuilderProvider: OAuth2RestClientBuilderProvider,
    private val googleDriveDocumentsStorageProperties: GoogleDriveDocumentsStorageProperties,
) {

    fun uploadFile(
        content: InputStreamProvider,
        fileName: String,
        parentFolderId: String,
    ): UploadFileResponse {
        val fileMetadata = GDriveCreateFileRequest(
            name = fileName,
            parents = listOf(parentFolderId),
            mimeType = "",
        )
        val restClient = createRestClient()

        return content.useInputStream { contentStream ->
                val contentResource = object : InputStreamResource(contentStream) {
                    override fun getFilename() = fileName
                    override fun contentLength() = -1L
                }
                val parts = LinkedMultiValueMap<String, Any>().apply {
                    add(
                        "metadata",
                        HttpEntity(
                            fileMetadata,
                            HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON },
                        )
                    )
                    add(
                        "media",
                        HttpEntity(
                            contentResource,
                            HttpHeaders().apply { contentType = MediaType.APPLICATION_OCTET_STREAM },
                        )
                    )
                }

                restClient.post()
                    .uri { builder ->
                        builder.path("upload/drive/v3/files")
                            .queryParam("fields", "id, size")
                            .queryParam("uploadType", "multipart")
                            .build()
                    }
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .accept(MediaType.APPLICATION_JSON)
                    .executeDriveRequest(
                        GDriveFile::class.java,
                        errorDescriptor = { errorJson ->
                            "Error while uploading $fileMetadata: $errorJson"
                        },
                    )
                    .toUploadFileResponse()
        }
    }

    fun downloadFile(fileId: String): InputStreamProvider {
        val restClient = createRestClient()
        return object : InputStreamProvider {
            override fun <T> useInputStream(consumer: (java.io.InputStream) -> T): T =
                restClient.get()
                    .uri { builder ->
                        builder.path("/drive/v3/files/$fileId")
                            .queryParam("alt", "media")
                            .build()
                    }
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchange { _, response ->
                        response.verifyDriveResponse(
                            successStatuses = setOf(HttpStatus.OK),
                            errorDescriptor = { errorJson ->
                                "Error while downloading $fileId: $errorJson"
                            },
                        )
                        response.body.use(consumer)
                    }!!
        }
    }

    fun deleteFile(fileId: String) {
        val restClient = createRestClient()
        restClient.delete()
                .uri { builder -> builder.path("/drive/v3/files/$fileId").build() }
                .executeDriveRequest(
                    successStatuses = setOf(HttpStatus.OK, HttpStatus.NO_CONTENT),
                    errorDescriptor = { errorJson -> "Error while deleting $fileId: $errorJson" },
                )
    }

    fun findFolderByNameAndParent(folderName: String, parentFolderId: String): String? {
        val restClient = createRestClient()
        val matchingFolders = restClient.get()
                .uri { builder ->
                    builder.path("/drive/v3/files")
                        .queryParam(
                            "q",
                            "'$parentFolderId' in parents and name = '$folderName' and trashed = false",
                        )
                        .build()
                }
                .accept(MediaType.APPLICATION_JSON)
                .executeDriveRequest(
                    GDriveFiles::class.java,
                    errorDescriptor = { errorJson ->
                        "Error while retrieving folder $folderName for $parentFolderId: $errorJson"
                    },
                )
        return matchingFolders.files.firstOrNull()?.id
    }

    fun createFolder(folderName: String, parentFolderId: String?): FolderResponse {
        log.debug { "Creating folder $folderName under $parentFolderId" }
        val restClient = createRestClient()
        return restClient.post()
                .uri { builder ->
                    builder.path("/drive/v3/files")
                        .queryParam("fields", "id, name")
                        .build()
                }
                .body(
                    GDriveCreateFileRequest(
                        name = folderName,
                        mimeType = "application/vnd.google-apps.folder",
                        parents = parentFolderId?.let(::listOf) ?: emptyList(),
                    )
                )
                .accept(MediaType.APPLICATION_JSON)
                .executeDriveRequest(
                    GDriveFile::class.java,
                    errorDescriptor = { errorJson ->
                        log.debug { "Error while creating folder $folderName: $errorJson" }
                        "Error while creating folder $folderName: $errorJson"
                    },
                )
                .toFolderResponse()
            .also { log.debug { "Folder $folderName created: $it" } }
    }

    fun getFolderById(folderId: String): FolderResponse? {
        log.debug { "Retrieving folder by id $folderId" }
        val restClient = createRestClient()
        return restClient.get()
                .uri { builder ->
                    builder.path("/drive/v3/files/$folderId")
                        .queryParam("fields", "name, trashed, id")
                        .build()
                }
                .accept(MediaType.APPLICATION_JSON)
                .executeDriveRequest(
                    GDriveFile::class.java,
                    errorDescriptor = { errorJson ->
                        log.debug { "Error while retrieving folder $folderId: $errorJson" }
                        "Error while retrieving folder $folderId: $errorJson"
                    },
                )
                .takeUnless { it.trashed == true }
                ?.toFolderResponse()
            .also { log.debug { "Folder $folderId retrieved: $it" } }
    }

    private fun createRestClient(): RestClient = try {
        restClientBuilderProvider.forClient(OAUTH2_CLIENT_REGISTRATION_ID)
            .baseUrl(googleDriveDocumentsStorageProperties.baseApiUrl)
            .build()
    } catch (e: OAuth2AuthorizationException) {
        log.debug { "Authorization error: ${e.message}" }
        throw StorageAuthorizationRequiredException(cause = e)
    }

    private fun <T : Any> RestClient.RequestHeadersSpec<*>.executeDriveRequest(
        responseType: Class<T>,
        successStatuses: Set<HttpStatus> = setOf(HttpStatus.OK),
        errorDescriptor: (String?) -> String,
    ): T {
        log.debug { "Executing request: $this" }
        return retrieve()
            .onStatus({ status -> status !in successStatuses }) { _, response ->
                throw response.toDriveException(successStatuses, errorDescriptor)
            }
            .body(responseType)!!
    }

    private fun RestClient.RequestHeadersSpec<*>.executeDriveRequest(
        successStatuses: Set<HttpStatus>,
        errorDescriptor: (String?) -> String,
    ) {
        log.debug { "Executing request: $this" }
        retrieve()
            .onStatus({ status -> status !in successStatuses }) { _, response ->
                throw response.toDriveException(successStatuses, errorDescriptor)
            }
            .toBodilessEntity()
    }
}

private fun ClientHttpResponse.verifyDriveResponse(
    successStatuses: Set<HttpStatus>,
    errorDescriptor: (String?) -> String,
) {
    if (statusCode !in successStatuses) {
        throw toDriveException(successStatuses, errorDescriptor)
    }
    log.debug { "Request executed successfully: $statusCode" }
}

private fun ClientHttpResponse.toDriveException(
    successStatuses: Set<HttpStatus>,
    errorDescriptor: (String?) -> String,
): DocumentStorageException {
    check(statusCode !in successStatuses)
    val errorJson = body.bufferedReader().use { it.readText() }.ifEmpty { null }
    return when (statusCode) {
        HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> {
            log.debug { "Authorization required: $statusCode" }
            StorageAuthorizationRequiredException(message = "Not authorized: $statusCode")
        }
        HttpStatus.NOT_FOUND -> {
            log.debug { "Resource not found: $errorJson" }
            DriveFileNotFoundException(errorJson)
        }
        else -> {
            log.debug { "Error response with code $statusCode: $errorJson" }
            DocumentStorageException(errorDescriptor(errorJson))
        }
    }
}

data class UploadFileResponse(
    val id: String,
    val sizeInBytes: Long?,
)

data class FolderResponse(
    val id: String,
    val name: String,
)

class DriveFileNotFoundException(message: String?) : DocumentStorageException(message)

private data class GDriveFiles(
    val files: List<GDriveFile>,
)

private data class GDriveFile(
    val id: String? = null,
    val size: Long? = null,
    val name: String? = null,
    val trashed: Boolean? = null,
) {
    fun toUploadFileResponse() = UploadFileResponse(id = id!!, sizeInBytes = size)
    fun toFolderResponse() = FolderResponse(id = id!!, name = name!!)
}

private data class GDriveCreateFileRequest(
    val name: String,
    val mimeType: String,
    val parents: List<String>,
)
