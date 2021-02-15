package io.orangebuffalo.simpleaccounting.domain.documents.storage.gdrive.impl

import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentStorageException
import io.orangebuffalo.simpleaccounting.domain.documents.storage.StorageAuthorizationRequiredException
import io.orangebuffalo.simpleaccounting.domain.documents.storage.gdrive.OAUTH2_CLIENT_REGISTRATION_ID
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2WebClientBuilderProvider
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
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
import reactor.core.publisher.Flux

@Component
class GoogleDriveApiAdapter(
    private val webClientBuilderProvider: OAuth2WebClientBuilderProvider,
    @Value("\${simpleaccounting.documents.storage.google-drive.base-api-url}") private val baseApiUrl: String
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
            .executeDriveRequest { errorJson ->
                "Error while uploading $fileMetadata: $errorJson"
            }
            .bodyToMono(GDriveFile::class.java)
            .map { driveFile -> driveFile.toUploadFileResponse() }
            .awaitSingle()
    }

    suspend fun downloadFile(fileId: String): Flux<DataBuffer> {
        return createWebClient()
            .get()
            .uri { builder ->
                builder.path("/drive/v3/files/$fileId")
                    .queryParam("alt", "media")
                    .build()
            }
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .executeDriveRequest { errorJson ->
                "Error while downloading $fileId: $errorJson"
            }
            .body(BodyExtractors.toDataBuffers())
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
            .executeDriveRequest { errorJson ->
                "Error while retrieving folder $folderName for $parentFolderId: $errorJson"
            }
            .bodyToMono(GDriveFiles::class.java)
            .awaitSingle()

        return if (matchingFolders.files.isEmpty()) null else matchingFolders.files[0].id
    }

    suspend fun createFolder(
        folderName: String,
        parentFolderId: String?
    ): FolderResponse = createWebClient()
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
        .executeDriveRequest { errorJson ->
            "Error while creating folder $folderName: $errorJson"
        }
        .bodyToMono(GDriveFile::class.java)
        .map { driveFile -> driveFile.toFolderResponse() }
        .awaitSingle()

    suspend fun getFolderById(folderId: String): FolderResponse? {
        return createWebClient()
            .get()
            .uri { builder ->
                builder.path("/drive/v3/files/$folderId")
                    .queryParam("fields", "name, trashed, id")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .executeDriveRequest { errorJson ->
                "Error while retrieving folder $folderId: $errorJson"
            }
            .bodyToMono(GDriveFile::class.java)
            .filter { driveFile -> !driveFile.trashed!! }
            .map { driveFile -> driveFile.toFolderResponse() }
            .awaitFirstOrNull()
    }

    private fun createWebClient() = webClientBuilderProvider
        .forClient(OAUTH2_CLIENT_REGISTRATION_ID)
        .baseUrl(baseApiUrl)
        .build()

    private suspend inline fun WebClient.RequestHeadersSpec<*>.executeDriveRequest(
        errorDescriptor: (errorJson: String?) -> String
    ): ClientResponse {
        val clientResponse = try {
            @Suppress("DEPRECATION")
            // Spring 5.3 does not provide coroutine-compatible API to achieve the sameSecurityUtils
            this.exchange().awaitSingle()
        } catch (e: OAuth2AuthorizationException) {
            throw StorageAuthorizationRequiredException(cause = e)
        }

        val statusCode = clientResponse.statusCode()
        if (statusCode == HttpStatus.UNAUTHORIZED || statusCode == HttpStatus.FORBIDDEN) {
            throw StorageAuthorizationRequiredException(message = "Not authorized: $statusCode")
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
