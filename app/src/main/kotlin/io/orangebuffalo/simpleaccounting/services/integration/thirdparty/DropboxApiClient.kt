package io.orangebuffalo.simpleaccounting.services.integration.thirdparty

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import mu.KotlinLogging
import java.nio.file.Path

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val logger = KotlinLogging.logger {}

class DropboxApiClient(
    accessToken: String,
    refreshToken: String,
    clientId: String,
    clientSecret: String
) : AutoCloseable {

    private val client = HttpClient(CIO) {
        dropboxClientConfig()
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
                }
                refreshTokens {
                    // dedicated client to disable Auth plugin
                    HttpClient(CIO) {
                        dropboxClientConfig()
                    }.use { refreshTokenClient ->
                        val tokenResponse = refreshTokenClient.submitForm(
                            url = "/oauth2/token",
                            formParameters = parameters {
                                append("grant_type", "refresh_token")
                                append("client_id", clientId)
                                append("client_secret", clientSecret)
                                append("refresh_token", oldTokens?.refreshToken ?: "")
                            },
                        ).body<TokenResponse>()

                        BearerTokens(accessToken = tokenResponse.accessToken, refreshToken = oldTokens?.refreshToken!!)
                    }
                }
            }
        }
    }

    override fun close() {
        client.close()
    }

    suspend fun uploadFile(backupFile: Path, filePath: String) = withContext(Dispatchers.IO) {
        logger.debug { "Uploading file $filePath to Dropbox" }

        val response = client.post {
            url("https://content.dropboxapi.com/2/files/upload")
            header(
                "Dropbox-API-Arg", UploadArg(path = filePath).toJson()
            )
            contentType(ContentType.Application.OctetStream)
            setBody(backupFile.toFile().readChannel())
        }.body<UploadResponse>()

        logger.debug { "File $filePath uploaded to Dropbox. Response: $response" }
    }

    suspend fun listFolder(folder: String, recursive: Boolean = true): List<ListFolderEntry> {
        logger.debug { "Listing folder $folder" }

        val allFiles = mutableListOf<ListFolderEntry>()
        var response = client.post {
            url("/2/files/list_folder")
            setBody(ListFolderRequest(path = folder, recursive = recursive))
        }.body<ListFolderArg>()
        allFiles.addAll(response.entries)

        while (response.hasMore) {
            logger.debug { "Listing folder $folder, cursor: ${response.cursor}" }

            val cursor = response.cursor
            response = client.post {
                url("/2/files/list_folder/continue")
                setBody(ListFolderContinueArg(cursor))
            }.body<ListFolderArg>()
            allFiles.addAll(response.entries)
        }

        logger.debug { "Folder $folder listed. Total files: ${allFiles.size}" }

        return allFiles
    }

    suspend fun deleteFiles(paths: Collection<String>) {
        logger.debug { "Deleting files $paths" }

        val deletionResult = client.post {
            url("/2/files/delete_batch")
            setBody(DeleteBatchArg(paths.map { DeleteBatchEntry(it) }))
        }.body<String>()

        logger.debug { "Files $paths deleted. Result: $deletionResult" }
    }
}

@Serializable
private data class UploadArg(
    val path: String,
    val mode: String = "add",
    @SerialName("autorename")
    val autoRename: Boolean = false,
    val mute: Boolean = true,
    @SerialName("strict_conflict")
    val strictConflict: Boolean = false
) {
    fun toJson(): String = json.encodeToString(serializer(), this)
}

@Serializable
private data class UploadResponse(
    @SerialName("name")
    val fileName: String,
    @SerialName("path_lower")
    val path: String,
    @SerialName("client_modified")
    val clientModified: String,
    @SerialName("server_modified")
    val serverModified: String,
    @SerialName("rev")
    val revision: String,
    @SerialName("size")
    val size: Long,
    @SerialName("id")
    val id: String,
    @SerialName("content_hash")
    val contentHash: String
)

@Serializable
private data class ListFolderRequest(
    val path: String = "/",
    val recursive: Boolean = false,
    @SerialName("include_media_info")
    val includeMediaInfo: Boolean = false,
    @SerialName("include_deleted")
    val includeDeleted: Boolean = false,
    @SerialName("include_has_explicit_shared_members")
    val includeHasExplicitSharedMembers: Boolean = false,
    @SerialName("include_mounted_folders")
    val includeMountedFolders: Boolean = false,
    @SerialName("include_non_downloadable_files")
    val includeNonDownloadableFiles: Boolean = false
)

@Serializable
private data class ListFolderArg(
    @SerialName("entries")
    val entries: List<ListFolderEntry> = emptyList(),
    @SerialName("cursor")
    val cursor: String,
    @SerialName("has_more")
    val hasMore: Boolean
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(".tag")
sealed interface ListFolderEntry

@Serializable
@SerialName("file")
data class FileListFolderEntry(
    @SerialName(".tag")
    val tag: String,
    @SerialName("name")
    val name: String,
    @SerialName("path_lower")
    val path: String,
    @SerialName("path_display")
    val pathDisplay: String,
    @SerialName("id")
    val id: String,
    @SerialName("client_modified")
    val clientModified: Instant,
    @SerialName("server_modified")
    val serverModified: Instant,
    @SerialName("rev")
    val revision: String,
    @SerialName("size")
    val size: Long,
    @SerialName("content_hash")
    val contentHash: String,
) : ListFolderEntry

@Suppress("unused")
@Serializable
@SerialName("folder")
data class FolderListFolderEntry(
    @SerialName(".tag")
    val tag: String,
    @SerialName("name")
    val name: String,
    @SerialName("path_lower")
    val path: String,
    @SerialName("path_display")
    val pathDisplay: String,
    @SerialName("id")
    val id: String
) : ListFolderEntry

@Serializable
private data class ListFolderContinueArg(
    @SerialName("cursor")
    val cursor: String
)

@Serializable
private data class DeleteBatchArg(
    val entries: List<DeleteBatchEntry>
)

@Serializable
private data class DeleteBatchEntry(
    val path: String
)

@Serializable
private data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long
)

/**
 * Dropbox requires /oauth2/token requests to be sent without Authorization, while Auth plugin cannot be disabled
 * on per-request basis. Thus, we need to use a different client when requesting short-lived access tokens.
 */
private fun HttpClientConfig<CIOEngineConfig>.dropboxClientConfig() {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json)
    }
    defaultRequest {
        url("https://api.dropboxapi.com")
        contentType(ContentType.Application.Json)
    }
}
