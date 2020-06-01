package io.orangebuffalo.simpleaccounting.services.storage.gdrive

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.stub
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.WithSaMockUser
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2ClientAuthorizationProvider
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2WebClientBuilderProvider
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.mockAccessToken
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.mockAuthorizationFailure
import io.orangebuffalo.simpleaccounting.services.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.services.storage.StorageAuthorizationRequiredException
import io.orangebuffalo.simpleaccounting.services.storage.StorageProviderResponse
import io.orangebuffalo.simpleaccounting.utils.NeedsWireMock
import io.orangebuffalo.simpleaccounting.utils.assertNumberOfStubbedRequests
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private val bufferFactory = DefaultDataBufferFactory()

@SimpleAccountingIntegrationTest
@NeedsWireMock
@TestPropertySource(
    properties = [
        "simpleaccounting.documents.storage.google-drive.base-api-url=http://localhost:\${wire-mock.port}"
    ]
)
class GoogleDriveDocumentsStorageServiceIT(
    @Autowired private val documentsStorageService: GoogleDriveDocumentsStorageService,
    @Autowired private val jdbcAggregateTemplate: JdbcAggregateTemplate
) {

    @MockBean
    lateinit var webClientBuilderProvider: OAuth2WebClientBuilderProvider

    @MockBean
    lateinit var clientAuthorizationProvider: OAuth2ClientAuthorizationProvider

    @AfterEach
    fun cleanup() {
        jdbcAggregateTemplate.deleteAll(GoogleDriveStorageIntegration::class.java)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should require authorization if client cannot be authorized`(testData: GoogleDriveTestData) {
        givenExistingDriveIntegration(testData)
        webClientBuilderProvider.mockAuthorizationFailure()
        clientAuthorizationProvider.stub {
            onBlocking { buildAuthorizationUrl(eq("google-drive"), any()) } doReturn "authUrl"
        }

        val status = whenCalculatingIntegrationStatus()

        assertUnauthorizedIntegrationStatus(status)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should require authorization if access token is invalid`(testData: GoogleDriveTestData) {
        givenExistingDriveIntegration(testData)
        webClientBuilderProvider.mockAccessToken("driveToken")
        clientAuthorizationProvider.stub {
            onBlocking { buildAuthorizationUrl(eq("google-drive"), any()) } doReturn "authUrl"
        }
        stubFor(
            get(urlPathEqualTo("/drive/v3/files/fryFolderId"))
                .withQueryParam("fields", equalTo("name, trashed, id"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .willReturn(unauthorized())
        )

        val status = whenCalculatingIntegrationStatus()

        assertUnauthorizedIntegrationStatus(status)
        assertNumberOfStubbedRequests(1)
    }

    private fun assertUnauthorizedIntegrationStatus(status: GoogleDriveStorageIntegrationStatus) {
        assertThat(status).isEqualTo(
            GoogleDriveStorageIntegrationStatus(
                folderName = null,
                folderId = "fryFolderId",
                authorizationRequired = true,
                authorizationUrl = "authUrl"
            )
        )
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should return existing root folder`(testData: GoogleDriveTestData) {
        givenExistingDriveIntegration(testData)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubFor(
            get(urlPathEqualTo("/drive/v3/files/fryFolderId"))
                .withQueryParam("fields", equalTo("name, trashed, id"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .willReturn(
                    okJson(
                        """{
                            "id": "fryFolderId",
                            "trashed": false,
                            "name": "fryFolder"
                        }"""
                    )
                )
        )

        val status = whenCalculatingIntegrationStatus()

        assertExistingIntegrationStatus(status)
        assertNumberOfStubbedRequests(1)
    }

    private fun assertExistingIntegrationStatus(status: GoogleDriveStorageIntegrationStatus) {
        assertThat(status).isEqualTo(
            GoogleDriveStorageIntegrationStatus(
                folderName = "fryFolder",
                folderId = "fryFolderId",
                authorizationRequired = false,
                authorizationUrl = null
            )
        )
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should create a new root folder if no previous integration exists`(testData: GoogleDriveTestData) {
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubNewRootFolderRequest()

        val status = whenCalculatingIntegrationStatus()

        assertNewIntegrationStatus(status)
        assertNumberOfStubbedRequests(1)
        assertNewIntegration(testData)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should create a new root folder if previously recorded is trashed`(testData: GoogleDriveTestData) {
        givenExistingDriveIntegration(testData)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubFor(
            get(urlPathEqualTo("/drive/v3/files/fryFolderId"))
                .withQueryParam("fields", equalTo("name, trashed, id"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .willReturn(
                    okJson(
                        """{
                            "id": "fryFolderId",
                            "trashed": true,
                            "name": "fryFolder"
                        }"""
                    )
                )
        )
        stubNewRootFolderRequest()

        val status = whenCalculatingIntegrationStatus()

        assertNewIntegrationStatus(status)
        assertNumberOfStubbedRequests(2)
        assertNewIntegration(testData)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should create a new root folder if previously recorded is not found`(testData: GoogleDriveTestData) {
        givenExistingDriveIntegration(testData)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubFor(
            get(urlPathEqualTo("/drive/v3/files/fryFolderId"))
                .withQueryParam("fields", equalTo("name, trashed, id"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .willReturn(notFound())
        )
        stubNewRootFolderRequest()

        val status = whenCalculatingIntegrationStatus()

        assertNewIntegrationStatus(status)
        assertNumberOfStubbedRequests(2)
        assertNewIntegration(testData)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should fail on getting content if OAuth2 client is not authorized`(testData: GoogleDriveTestData) {
        webClientBuilderProvider.mockAuthorizationFailure()

        assertThatThrownBy {
            whenDownloadingDocumentContent(testData)
        }.isInstanceOf(StorageAuthorizationRequiredException::class.java)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should download file content`(testData: GoogleDriveTestData) {
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubFor(
            get(urlPathEqualTo("/drive/v3/files/testLocation"))
                .withQueryParam("alt", equalTo("media"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                        .withBody("Test Content".toByteArray())
                )
        )

        val contentBuffers = whenDownloadingDocumentContent(testData)

        assertNumberOfStubbedRequests(1)
        assertThat(convertResponseToString(contentBuffers)).isEqualTo("Test Content")
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should fail on saving content if integration is not configured`(testData: GoogleDriveTestData) {
        assertThatThrownBy {
            whenSavingDocument(testData)
        }.isInstanceOf(StorageAuthorizationRequiredException::class.java)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should fail on saving content if OAuth2 client is not authorized`(testData: GoogleDriveTestData) {
        givenExistingDriveIntegration(testData)
        webClientBuilderProvider.mockAuthorizationFailure()

        assertThatThrownBy {
            whenSavingDocument(testData)
        }.isInstanceOf(StorageAuthorizationRequiredException::class.java)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should successfully upload a new document`(testData: GoogleDriveTestData) {
        givenExistingDriveIntegration(testData)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubGetWorkspaceFolder(
            testData,
            """{
                "files": [{
                    "id": "fryWorkspaceFolderId",
                    "name": "fryWorkspaceFolder"
                }]
            }"""
        )
        stubNewFileUpload("fryWorkspaceFolderId")

        val documentResponse = whenSavingDocument(testData)

        assertNumberOfStubbedRequests(2)
        assertThat(documentResponse).isEqualTo(
            StorageProviderResponse(
                storageProviderLocation = "newFryFileId",
                sizeInBytes = 42
            )
        )
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should create a workspace folder if not exists during document upload`(testData: GoogleDriveTestData) {
        givenExistingDriveIntegration(testData)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubGetWorkspaceFolder(testData, """{ "files": [] } """)
        stubFor(
            post(urlPathEqualTo("/drive/v3/files"))
                .withQueryParam("fields", equalTo("id, name"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .withRequestBody(
                    equalToJson(
                        """{
                                "name": "${testData.workspace.id}",
                                "mimeType": "application/vnd.google-apps.folder",
                                "parents": ["fryFolderId"]
                            }"""
                    )
                )
                .willReturn(
                    okJson(
                        """{ 
                            "id": "workspaceFolderId", 
                            "name": "${testData.workspace.id}"
                        }"""
                    )
                )
        )
        stubNewFileUpload("workspaceFolderId")

        val documentResponse = whenSavingDocument(testData)

        assertNumberOfStubbedRequests(3)
        assertThat(documentResponse).isEqualTo(
            StorageProviderResponse(
                storageProviderLocation = "newFryFileId",
                sizeInBytes = 42
            )
        )
    }

    private fun stubGetWorkspaceFolder(testData: GoogleDriveTestData, responseJson: String) {
        stubFor(
            get(urlPathEqualTo("/drive/v3/files"))
                .withQueryParam(
                    "q",
                    equalTo("'fryFolderId' in parents and name = '${testData.workspace.id}' and trashed = false")
                )
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .willReturn(okJson(responseJson))
        )
    }

    private fun stubNewFileUpload(parent: String) {
        stubFor(
            post(urlPathEqualTo("/upload/drive/v3/files"))
                .withQueryParam("fields", equalTo("id, size"))
                .withQueryParam("uploadType", equalTo("multipart"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .withHeader(HttpHeaders.CONTENT_TYPE, matching("${MediaType.MULTIPART_FORM_DATA_VALUE}.*"))
                .withMultipartRequestBody(
                    aMultipart("metadata")
                        .withBody(
                            equalToJson(
                                """{
                                    "name": "testFileName",
                                    "parents": ["$parent"],
                                    "mimeType": ""     
                                }"""
                            )
                        )
                )
                .withMultipartRequestBody(
                    aMultipart("media").withBody(equalTo("Document Body"))
                )
                .willReturn(
                    okJson(
                        """{
                                "id": "newFryFileId",
                                "size": 42
                            }"""
                    )
                )
        )
    }

    private fun convertResponseToString(contentBuffers: Flux<DataBuffer>): String {
        val os = ByteArrayOutputStream()
        DataBufferUtils.write(contentBuffers, os)
            .map { DataBufferUtils.release(it) }
            .blockLast()
        return String(os.toByteArray())
    }

    private fun whenDownloadingDocumentContent(testData: GoogleDriveTestData): Flux<DataBuffer> {
        return runBlocking {
            documentsStorageService.getDocumentContent(testData.workspace, "testLocation")
        }
    }

    private fun whenSavingDocument(testData: GoogleDriveTestData): StorageProviderResponse {
        val documentBody = ByteArrayInputStream("Document Body".toByteArray())
        return runBlocking {
            documentsStorageService.saveDocument(
                SaveDocumentRequest(
                    fileName = "testFileName",
                    workspace = testData.workspace,
                    content = DataBufferUtils.readInputStream({ documentBody }, bufferFactory, 4096)
                )
            )
        }
    }

    private fun assertNewIntegrationStatus(status: GoogleDriveStorageIntegrationStatus) {
        assertThat(status).isEqualTo(
            GoogleDriveStorageIntegrationStatus(
                folderName = "newFolder",
                folderId = "newFolderId",
                authorizationRequired = false,
                authorizationUrl = null
            )
        )
    }

    private fun stubNewRootFolderRequest() {
        stubFor(
            post(urlPathEqualTo("/drive/v3/files"))
                .withQueryParam("fields", equalTo("id, name"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
                .withRequestBody(
                    equalToJson(
                        """{
                                "name": "simple-accounting",
                                "mimeType": "application/vnd.google-apps.folder",
                                "parents": []
                            }"""
                    )
                )
                .willReturn(
                    okJson(
                        """{
                                "id": "newFolderId",
                                "name": "newFolder"
                            }"""
                    )
                )
        )
    }

    private fun whenCalculatingIntegrationStatus() =
        runBlocking { documentsStorageService.getCurrentUserIntegrationStatus() }

    private fun assertNewIntegration(testData: GoogleDriveTestData) {
        assertThat(jdbcAggregateTemplate.findAll(GoogleDriveStorageIntegration::class.java))
            .hasOnlyOneElementSatisfying { integration ->
                assertThat(integration.userId).isEqualTo(testData.fry.id!!)
                assertThat(integration.folderId).isEqualTo("newFolderId")
            }
    }

    private fun givenExistingDriveIntegration(testData: GoogleDriveTestData) {
        jdbcAggregateTemplate.save(
            GoogleDriveStorageIntegration(
                userId = testData.fry.id!!,
                folderId = "fryFolderId"
            )
        )
    }

    class GoogleDriveTestData : TestData {
        val fry = Prototypes.fry()
        val workspace = Prototypes.workspace(owner = fry)
        override fun generateData() = listOf(fry, workspace)
    }

}
