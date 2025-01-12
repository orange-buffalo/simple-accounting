package io.orangebuffalo.simpleaccounting.infra.thirdparty.dropbox

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.tests.infra.api.stubPostRequestTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.willReturnOkJson
import io.orangebuffalo.simpleaccounting.tests.infra.api.willReturnResponse
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@WireMockTest
internal class DropboxApiClientTest {

    private lateinit var apiClient: DropboxApiClient

    @TempDir
    private lateinit var tempDir: Path

    @BeforeEach
    fun setup(wmRuntimeInfo: WireMockRuntimeInfo) {
        val port = wmRuntimeInfo.httpPort
        apiClient = DropboxApiClient(
            accessToken = "TestAccessToken",
            refreshToken = "TestRefreshToken",
            clientId = "TestClientId",
            clientSecret = "TestClientSecret",
            apiBaseUrl = "http://localhost:$port",
            contentBaseUrl = "http://localhost:$port",
        )
    }

    @Test
    fun `should upload file`(): Unit = runBlocking {
        // Create a temp file and write some content to it
        val tempFile = Files.createFile(tempDir.resolve("tempFile.txt"))
        Files.writeString(tempFile, "This is a temp file")

        stubPostRequestTo("/2/files/upload") {
            withRequestBody(equalTo("This is a temp file"))
            withHeader("Authorization", equalTo("Bearer TestAccessToken"))
            withHeader("Content-Type", equalTo("application/octet-stream"))
            withHeader(
                "Dropbox-API-Arg", equalToJson(
                    """{
                            "path": "/your/destination/path",
                            "mode": "add",
                            "autorename": false,
                            "mute": true,
                            "strict_conflict": false
                        }"""
                )
            )
            willReturnOkJson(/*language=json*/
                """{
                    "name": "uploaded.txt",
                    "path_lower": "/uploaded.txt",
                    "path_display": "/Uploaded.txt",
                    "id": "id_upload",
                    "client_modified": "2023-09-02T00:00:00Z",
                    "server_modified": "2023-09-02T00:00:00Z",
                    "rev": "345",
                    "size": 19,
                    "content_hash": "hash_upload"
                }"""
            )
        }

        apiClient.uploadFile(tempFile, "/your/destination/path")

        verify(exactly(1), postRequestedFor(urlEqualTo("/2/files/upload")))
    }

    @Test
    fun `should refresh token on upload file`(): Unit = runBlocking {
        // Create a temp file and write some content to it
        val tempFile = Files.createFile(tempDir.resolve("tempFile.txt"))
        Files.writeString(tempFile, "This is a temp file")

        stubExpiredTokenForRequestTo("/2/files/upload")
        stubNewTokenRequest()
        stubPostRequestTo("/2/files/upload") {
            withRequestBody(equalTo("This is a temp file"))
            withHeader("Authorization", equalTo("Bearer NewToken"))
            withHeader("Content-Type", equalTo("application/octet-stream"))
            withHeader(
                "Dropbox-API-Arg", equalToJson(
                    """{
                            "path": "/your/destination/path",
                            "mode": "add",
                            "autorename": false,
                            "mute": true,
                            "strict_conflict": false
                        }"""
                )
            )
            willReturnOkJson(/*language=json*/
                """{
                    "name": "uploaded.txt",
                    "path_lower": "/uploaded.txt",
                    "path_display": "/Uploaded.txt",
                    "id": "id_upload",
                    "client_modified": "2023-09-02T00:00:00Z",
                    "server_modified": "2023-09-02T00:00:00Z",
                    "rev": "345",
                    "size": 19,
                    "content_hash": "hash_upload"
                }"""
            )
        }

        apiClient.uploadFile(tempFile, "/your/destination/path")

        verify(exactly(2), postRequestedFor(urlEqualTo("/2/files/upload")))
        verify(exactly(1), postRequestedFor(urlEqualTo("/oauth2/token")))
    }

    @Test
    fun `should list folder`(): Unit = runBlocking {
        stubPostRequestTo("/2/files/list_folder") {
            withHeader("Authorization", equalTo("Bearer TestAccessToken"))
            withHeader("Content-Type", equalTo("application/json"))
            willReturnOkJson(/*language=json*/
                """{
                    "entries": [
                        {
                            ".tag": "file",
                            "name": "file1.txt",
                            "path_lower": "/file1",
                            "path_display": "/File1",
                            "id": "id_1",
                            "client_modified": "2021-09-01T00:00:00Z",
                            "server_modified": "2021-09-01T00:00:00Z",
                            "rev": "123",
                            "size": 1024,
                            "content_hash": "hash1"
                        },
                        {
                            ".tag": "file",
                            "name": "file2.txt",
                            "path_lower": "/file2",
                            "path_display": "/File2",
                            "id": "id_2",
                            "client_modified": "2021-09-02T00:00:00Z",
                            "server_modified": "2021-09-02T00:00:00Z",
                            "rev": "124",
                            "size": 2048,
                            "content_hash": "hash2"
                        }
                    ],
                    "cursor": "some_cursor",
                    "has_more": false
                }"""
            )
        }

        val listFolderResult = apiClient.listFolder("/some_folder")

        listFolderResult.shouldContainExactly(
            FileListFolderEntry(
                tag = "file",
                name = "file1.txt",
                path = "/file1",
                pathDisplay = "/File1",
                id = "id_1",
                clientModified = Instant.parse("2021-09-01T00:00:00Z"),
                serverModified = Instant.parse("2021-09-01T00:00:00Z"),
                revision = "123",
                size = 1024,
                contentHash = "hash1"
            ),
            FileListFolderEntry(
                tag = "file",
                name = "file2.txt",
                path = "/file2",
                pathDisplay = "/File2",
                id = "id_2",
                clientModified = Instant.parse("2021-09-02T00:00:00Z"),
                serverModified = Instant.parse("2021-09-02T00:00:00Z"),
                revision = "124",
                size = 2048,
                contentHash = "hash2"
            )
        )

        verify(exactly(1), postRequestedFor(urlEqualTo("/2/files/list_folder")))
    }

    @Test
    fun `should refresh token on list folder`(): Unit = runBlocking {
        stubExpiredTokenForRequestTo("/2/files/list_folder")
        stubNewTokenRequest()
        stubPostRequestTo("/2/files/list_folder") {
            withHeader("Authorization", equalTo("Bearer NewToken"))
            withHeader("Content-Type", equalTo("application/json"))
            willReturnOkJson(/*language=json*/
                """{
                    "entries": [
                        {
                            ".tag": "file",
                            "name": "file1.txt",
                            "path_lower": "/file1",
                            "path_display": "/File1",
                            "id": "id_1",
                            "client_modified": "2021-09-01T00:00:00Z",
                            "server_modified": "2021-09-01T00:00:00Z",
                            "rev": "123",
                            "size": 1024,
                            "content_hash": "hash1"
                        }
                    ],
                    "cursor": "some_cursor",
                    "has_more": false
                }"""
            )
        }

        val listFolderResult = apiClient.listFolder("/some_folder")

        listFolderResult.shouldContainExactly(
            FileListFolderEntry(
                tag = "file",
                name = "file1.txt",
                path = "/file1",
                pathDisplay = "/File1",
                id = "id_1",
                clientModified = Instant.parse("2021-09-01T00:00:00Z"),
                serverModified = Instant.parse("2021-09-01T00:00:00Z"),
                revision = "123",
                size = 1024,
                contentHash = "hash1"
            )
        )

        verify(exactly(2), postRequestedFor(urlEqualTo("/2/files/list_folder")))
        verify(postRequestedFor(urlEqualTo("/oauth2/token")))
    }

    private fun stubNewTokenRequest() {
        stubPostRequestTo("/oauth2/token") {
            withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
            withRequestBody(
                equalTo(
                    "grant_type=refresh_token&client_id=TestClientId&client_secret=TestClientSecret&refresh_token=TestRefreshToken"
                )
            )
            willReturnOkJson(
                """{ 
                        "access_token": "NewToken", 
                        "expires_in": 3600 }
                    """
            )
        }
    }

    private fun stubExpiredTokenForRequestTo(url: String) {
        stubPostRequestTo(url) {
            withHeader("Authorization", equalTo("Bearer TestAccessToken"))
            willReturnResponse {
                withStatus(401)
                withHeader("Content-Type", "application/json")
                withBody(
                    """{
                            "error_summary": "expired_access_token/...",
                            "error": {
                                ".tag": "expired_access_token"
                            }
                        }"""
                )
            }
        }
    }

    @Test
    fun `should list folder with pagination`(): Unit = runBlocking {
        // First page
        stubPostRequestTo("/2/files/list_folder") {
            withHeader("Authorization", equalTo("Bearer TestAccessToken"))
            withHeader("Content-Type", equalTo("application/json"))
            willReturnOkJson(/*language=json*/
                """{
                    "entries": [
                        {
                            ".tag": "file",
                            "name": "file1.txt",
                            "path_lower": "/file1",
                            "path_display": "/File1",
                            "id": "id_1",
                            "client_modified": "2022-09-02T00:00:00Z",
                            "server_modified": "2022-09-02T00:00:01Z",
                            "rev": "1",
                            "size": 100,
                            "content_hash": "hash_1"
                        },
                        {
                            ".tag": "file",
                            "name": "file2.txt",
                            "path_lower": "/file2",
                            "path_display": "/File2",
                            "id": "id_2",
                            "client_modified": "2022-09-02T00:00:02Z",
                            "server_modified": "2022-09-02T00:00:03Z",
                            "rev": "2",
                            "size": 200,
                            "content_hash": "hash_2"
                        }
                    ],
                    "cursor": "cursor_1",
                    "has_more": true
                }"""
            )
        }

        // Second page
        stubPostRequestTo("/2/files/list_folder/continue") {
            withHeader("Authorization", equalTo("Bearer TestAccessToken"))
            withHeader("Content-Type", equalTo("application/json"))
            willReturnOkJson(/*language=json*/
                """{
                    "entries": [
                        {
                            ".tag": "file",
                            "name": "file3.txt",
                            "path_lower": "/file3",
                            "path_display": "/File3",
                            "id": "id_3",
                            "client_modified": "2022-09-02T00:00:04Z",
                            "server_modified": "2022-09-02T00:00:05Z",
                            "rev": "3",
                            "size": 300,
                            "content_hash": "hash_3"
                        }
                    ],
                    "cursor": "cursor_2",
                    "has_more": false
                }"""
            )
        }

        val files = apiClient.listFolder("/")

        files.shouldContainExactly(
            FileListFolderEntry(
                tag = "file",
                name = "file1.txt",
                path = "/file1",
                pathDisplay = "/File1",
                id = "id_1",
                clientModified = Instant.parse("2022-09-02T00:00:00Z"),
                serverModified = Instant.parse("2022-09-02T00:00:01Z"),
                revision = "1",
                size = 100,
                contentHash = "hash_1"
            ),
            FileListFolderEntry(
                tag = "file",
                name = "file2.txt",
                path = "/file2",
                pathDisplay = "/File2",
                id = "id_2",
                clientModified = Instant.parse("2022-09-02T00:00:02Z"),
                serverModified = Instant.parse("2022-09-02T00:00:03Z"),
                revision = "2",
                size = 200,
                contentHash = "hash_2"
            ),
            FileListFolderEntry(
                tag = "file",
                name = "file3.txt",
                path = "/file3",
                pathDisplay = "/File3",
                id = "id_3",
                clientModified = Instant.parse("2022-09-02T00:00:04Z"),
                serverModified = Instant.parse("2022-09-02T00:00:05Z"),
                revision = "3",
                size = 300,
                contentHash = "hash_3"
            )
        )

        verify(exactly(1), postRequestedFor(urlEqualTo("/2/files/list_folder")))
        verify(exactly(1), postRequestedFor(urlEqualTo("/2/files/list_folder/continue")))
    }

    @Test
    fun `should delete files`(): Unit = runBlocking {
        stubPostRequestTo("/2/files/delete_batch") {
            withHeader("Authorization", equalTo("Bearer TestAccessToken"))
            withHeader("Content-Type", equalTo("application/json"))
            willReturnOkJson("{}")
        }

        apiClient.deleteFiles(listOf("/file1", "/file2"))

        verify(exactly(1), postRequestedFor(urlEqualTo("/2/files/delete_batch")))
    }

    @Test
    fun `should refresh token on delete files`(): Unit = runBlocking {
        stubExpiredTokenForRequestTo("/2/files/delete_batch")
        stubNewTokenRequest()
        stubPostRequestTo("/2/files/delete_batch") {
            withHeader("Authorization", equalTo("Bearer NewToken"))
            withHeader("Content-Type", equalTo("application/json"))
            withRequestBody(
                equalToJson(
                    """{
                        "entries":[
                          { "path": "/file1" },
                          { "path": "/file2" }
                        ]
                    }"""
                )
            )
            willReturnOkJson("{}")
        }

        apiClient.deleteFiles(listOf("/file1", "/file2"))

        verify(exactly(2), postRequestedFor(urlEqualTo("/2/files/delete_batch")))
        verify(exactly(1), postRequestedFor(urlEqualTo("/oauth2/token")))
    }
}
