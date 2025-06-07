package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import com.github.tomakehurst.wiremock.client.WireMock.*
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
                                requestParents.forEach { put(it) }
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

    fun verifyFindFileRequest(
        fileId: String,
    ) {
        wireMockServer.verify(
            getRequestedFor(urlPathMatching("$GDRIVE_MOCKS_ROOT_PATH/drive/v3/files/$fileId"))
        )
    }
}
