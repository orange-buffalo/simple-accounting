package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

private val wireMockServer = ThirdPartyApisMocks.server

object GoogleDriveApiMocks {
    private const val GDRIVE_MOCKS_ROOT_PATH = "/google-drive-mocks"

    fun configProperties() = arrayOf(
        "simpleaccounting.documents.storage.google-drive.base-api-url=" +
                "http://localhost:${wireMockServer.port()}$GDRIVE_MOCKS_ROOT_PATH"
    )

    fun mockCreateFolder(
        requestName: String,
        requestParents: List<String>,
        responseId: String,
        expectedAuthToken: OAuthMocksToken,
    ) {
        wireMockServer.stubFor(
            post(urlPathMatching("$GDRIVE_MOCKS_ROOT_PATH/drive/v3/files"))
                .withQueryParam("fields", equalTo("id, name"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, expectedAuthToken.authorizationHeaderMatcher())
                .withRequestBody(
                    equalToJson(
                        buildJsonObject {
                            put("name", requestName)
                            putJsonArray("parents") {
                                requestParents.forEach { add(it) }
                            }
                            put("mimeType", "application/vnd.google-apps.folder")
                        }.toString()
                    )
                )
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            buildJsonObject {
                                put("id", responseId)
                                put("name", requestName)
                            }.toString()
                        )
                )
        )
    }

    fun mockFindFile(
        fileId: String,
        fileName: String = "name-of-$fileId",
        trashed: Boolean = false,
        expectedAuthToken: OAuthMocksToken,
        send404Response: Boolean = false,
    ) {
        wireMockServer.stubFor(
            get(urlPathMatching("$GDRIVE_MOCKS_ROOT_PATH/drive/v3/files/$fileId"))
                .withQueryParam("fields", equalTo("name, trashed, id"))
                .withHeader(HttpHeaders.AUTHORIZATION, expectedAuthToken.authorizationHeaderMatcher())
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .apply {
                            if (send404Response) {
                                withStatus(404)
                            } else {
                                withBody(
                                    buildJsonObject {
                                        put("id", fileId)
                                        put("name", fileName)
                                        put("trashed", trashed)
                                    }.toString()
                                )
                            }
                        }
                )
        )
    }

    fun mockUploadFile(
        responseId: String,
        responseSize: Long,
        expectedAuthToken: OAuthMocksToken,
    ) {
        wireMockServer.stubFor(
            post(urlPathMatching("${GDRIVE_MOCKS_ROOT_PATH}upload/drive/v3/files"))
                .withQueryParam("fields", equalTo("id, size"))
                .withQueryParam("uploadType", equalTo("multipart"))
                .withHeader(HttpHeaders.AUTHORIZATION, expectedAuthToken.authorizationHeaderMatcher())
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            buildJsonObject {
                                put("id", responseId)
                                put("size", responseSize)
                            }.toString()
                        )
                )
        )
    }

    fun mockUploadFileSequence(
        responses: List<UploadFileResponse>,
        expectedAuthToken: OAuthMocksToken,
    ) {
        val scenarioName = "upload-file-sequence"
        responses.forEachIndexed { index, response ->
            val currentState = if (index == 0) Scenario.STARTED else "upload-$index"
            val nextState = "upload-${index + 1}"
            wireMockServer.stubFor(
                post(urlPathMatching("${GDRIVE_MOCKS_ROOT_PATH}upload/drive/v3/files"))
                    .inScenario(scenarioName)
                    .whenScenarioStateIs(currentState)
                    .willSetStateTo(nextState)
                    .withQueryParam("fields", equalTo("id, size"))
                    .withQueryParam("uploadType", equalTo("multipart"))
                    .withHeader(HttpHeaders.AUTHORIZATION, expectedAuthToken.authorizationHeaderMatcher())
                    .willReturn(
                        aResponse()
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(
                                buildJsonObject {
                                    put("id", response.id)
                                    put("size", response.size)
                                }.toString()
                            )
                    )
            )
        }
    }

    data class UploadFileResponse(
        val id: String,
        val size: Long,
    )

    fun mockDownloadFile(
        fileId: String,
        content: ByteArray,
        expectedAuthToken: OAuthMocksToken,
    ) {
        wireMockServer.stubFor(
            get(urlPathMatching("$GDRIVE_MOCKS_ROOT_PATH/drive/v3/files/$fileId"))
                .withQueryParam("alt", equalTo("media"))
                .withHeader(HttpHeaders.AUTHORIZATION, expectedAuthToken.authorizationHeaderMatcher())
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                        .withBody(content)
                )
        )
    }

    fun mockFindFolder(
        parentFolderId: String,
        folderName: String,
        responseFolderId: String? = null,
        expectedAuthToken: OAuthMocksToken,
    ) {
        wireMockServer.stubFor(
            get(urlPathMatching("$GDRIVE_MOCKS_ROOT_PATH/drive/v3/files"))
                .withQueryParam(
                    "q",
                    equalTo("'$parentFolderId' in parents and name = '$folderName' and trashed = false")
                )
                .withHeader(HttpHeaders.AUTHORIZATION, expectedAuthToken.authorizationHeaderMatcher())
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            buildJsonObject {
                                putJsonArray("files") {
                                    if (responseFolderId != null) {
                                        addJsonObject {
                                            put("id", responseFolderId)
                                        }
                                    }
                                }
                            }.toString()
                        )
                )
        )
    }

    fun verifyFindFileRequest(
        fileId: String,
    ) {
        wireMockServer.verify(
            getRequestedFor(urlPathMatching("$GDRIVE_MOCKS_ROOT_PATH/drive/v3/files/$fileId"))
        )
    }

    fun verifyUploadFileRequest() {
        wireMockServer.verify(
            postRequestedFor(urlPathMatching("${GDRIVE_MOCKS_ROOT_PATH}upload/drive/v3/files"))
        )
    }

    fun verifyCreateFolderRequest(
        folderName: String,
        parentFolderId: String,
    ) {
        wireMockServer.verify(
            postRequestedFor(urlPathMatching("$GDRIVE_MOCKS_ROOT_PATH/drive/v3/files"))
                .withRequestBody(
                    matchingJsonPath("$.name", equalTo(folderName))
                )
                .withRequestBody(
                    matchingJsonPath("$.parents[0]", equalTo(parentFolderId))
                )
        )
    }
}
