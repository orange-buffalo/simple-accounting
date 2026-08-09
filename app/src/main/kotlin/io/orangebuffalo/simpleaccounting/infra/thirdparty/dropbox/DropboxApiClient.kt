package io.orangebuffalo.simpleaccounting.infra.thirdparty.dropbox

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.time.Instant

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val logger = KotlinLogging.logger {}

class DropboxApiClient(
    accessToken: String,
    private val refreshToken: String,
    private val clientId: String,
    private val clientSecret: String,
    apiBaseUrl: String = "https://api.dropboxapi.com",
    private val contentBaseUrl: String = "https://content.dropboxapi.com",
) : AutoCloseable {

    private val apiClient = RestClient.builder().baseUrl(apiBaseUrl).build()
    private val contentClient = RestClient.create()
    private val refreshLock = Any()

    @Volatile
    private var currentAccessToken = accessToken

    override fun close() = Unit

    fun uploadFile(fileToUpload: Path, filePath: String) {
        logger.debug { "Uploading file $filePath to Dropbox" }

        val response = authorizedRequest { token ->
            contentClient.post()
                .uri("$contentBaseUrl/2/files/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .header("Dropbox-API-Arg", UploadArg(path = filePath).toJson())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileToUpload.readBytes())
                .retrieve()
                .body(String::class.java)
        }

        logger.debug {
            "File $filePath uploaded to Dropbox. Response: ${json.decodeFromString<UploadResponse>(response!!)}"
        }
    }

    fun listFolder(folder: String, recursive: Boolean = true): List<ListFolderEntry> {
        logger.debug { "Listing folder $folder" }

        val allFiles = mutableListOf<ListFolderEntry>()
        var response = postJson<ListFolderRequest, ListFolderArg>(
            "/2/files/list_folder",
            ListFolderRequest(path = folder, recursive = recursive),
        )
        allFiles.addAll(response.entries)

        while (response.hasMore) {
            logger.debug { "Listing folder $folder, cursor: ${response.cursor}" }

            val cursor = response.cursor
            response = postJson<ListFolderContinueArg, ListFolderArg>(
                "/2/files/list_folder/continue",
                ListFolderContinueArg(cursor),
            )
            allFiles.addAll(response.entries)
        }

        logger.debug { "Folder $folder listed. Total files: ${allFiles.size}" }

        return allFiles
    }

    fun deleteFiles(paths: Collection<String>) {
        logger.debug { "Deleting files $paths" }

        val deletionResult = postJson<DeleteBatchArg, String>(
            "/2/files/delete_batch",
            DeleteBatchArg(paths.map { DeleteBatchEntry(it) }),
        )

        logger.debug { "Files $paths deleted. Result: $deletionResult" }
    }

    private inline fun <reified T, reified R> postJson(path: String, request: T): R {
        val response = authorizedRequest { token ->
            apiClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.encodeToString(request))
                .retrieve()
                .body(String::class.java)
        }
        return if (R::class == String::class) {
            @Suppress("UNCHECKED_CAST")
            response as R
        } else {
            json.decodeFromString(response!!)
        }
    }

    private fun <T> authorizedRequest(request: (String) -> T): T {
        val token = currentAccessToken
        return try {
            request(token)
        } catch (error: HttpClientErrorException.Unauthorized) {
            refreshAccessToken(token)
            request(currentAccessToken)
        }
    }

    private fun refreshAccessToken(rejectedToken: String) {
        synchronized(refreshLock) {
            if (currentAccessToken != rejectedToken) return

            val form = LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "refresh_token")
                add("client_id", clientId)
                add("client_secret", clientSecret)
                add("refresh_token", refreshToken)
            }
            val response = apiClient.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String::class.java)
            currentAccessToken = json.decodeFromString<TokenResponse>(response!!).accessToken
        }
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
    @Serializable(with = InstantIso8601Serializer::class)
    val clientModified: Instant,
    @SerialName("server_modified")
    @Serializable(with = InstantIso8601Serializer::class)
    val serverModified: Instant,
    @SerialName("rev")
    val revision: String,
    @SerialName("size")
    val size: Long,
    @SerialName("content_hash")
    val contentHash: String,
) : ListFolderEntry

private object InstantIso8601Serializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
}

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
