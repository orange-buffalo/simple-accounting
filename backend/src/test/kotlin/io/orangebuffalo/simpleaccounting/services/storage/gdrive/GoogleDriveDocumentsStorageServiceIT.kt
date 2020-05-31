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
import io.orangebuffalo.simpleaccounting.utils.NeedsWireMock
import io.orangebuffalo.simpleaccounting.utils.assertNumberOfStubbedRequests
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource

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
        override fun generateData() = listOf(fry)
    }

}
