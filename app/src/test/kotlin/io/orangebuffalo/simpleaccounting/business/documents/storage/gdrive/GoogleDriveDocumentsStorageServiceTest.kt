package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.documents.storage.StorageAuthorizationRequiredException
import io.orangebuffalo.simpleaccounting.business.integration.pushnotifications.PushNotificationService
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.oauth2.*
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithSaMockUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.consumeToString
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.io.ByteArrayInputStream
import java.util.function.Consumer

private val bufferFactory = DefaultDataBufferFactory()

/**
 * TODO: this test is mostly covered by [io.orangebuffalo.simpleaccounting.business.ui.user.profile.UserProfileGoogleDriveDocumentStorageFullStackTest].
 * The remaining parts to be covered by the pages that use upload / download functionality.
 */
@Deprecated("Uses excessive mocking, should be replaced with a full stack test")
@NeedsWireMock
@TestPropertySource(
    properties = [
        "simpleaccounting.documents.storage.google-drive.base-api-url=http://localhost:\${wire-mock.port}"
    ]
)
class GoogleDriveDocumentsStorageServiceTest(
    @Autowired private val documentsStorage: GoogleDriveDocumentsStorage,
    @Autowired private val jdbcAggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val applicationEventPublisher: ApplicationEventPublisher,
) : SaIntegrationTestBase() {

    @MockitoBean
    lateinit var webClientBuilderProvider: OAuth2WebClientBuilderProvider

    @MockitoBean
    lateinit var clientAuthorizationProvider: OAuth2ClientAuthorizationProvider

    @MockitoBean
    lateinit var pushNotificationService: PushNotificationService

    @AfterEach
    fun cleanup() {
        jdbcAggregateTemplate.deleteAll(GoogleDriveStorageIntegration::class.java)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should require authorization if client cannot be authorized`() {
        givenExistingDriveIntegration(preconditions.fry)
        webClientBuilderProvider.mockAuthorizationFailure()
        clientAuthorizationProvider.stub {
            onBlocking { buildAuthorizationUrl(eq("google-drive"), any()) } doReturn "authUrl"
        }

        val status = whenCalculatingIntegrationStatus()

        assertUnauthorizedIntegrationStatus(status)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should require authorization if access token is invalid`() {
        givenExistingDriveIntegration(preconditions.fry)
        webClientBuilderProvider.mockAccessToken("driveToken")
        clientAuthorizationProvider.stub {
            onBlocking { buildAuthorizationUrl(eq("google-drive"), any()) } doReturn "authUrl"
        }
        stubGetRequestTo("/drive/v3/files/fryFolderId") {
            withQueryParam("fields", equalTo("name, trashed, id"))
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            willReturn(unauthorized())
        }

        val status = whenCalculatingIntegrationStatus()

        assertUnauthorizedIntegrationStatus(status)
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
    fun `should return existing root folder`() {
        givenExistingDriveIntegration(preconditions.fry)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubExistingRootFolder()

        val status = whenCalculatingIntegrationStatus()

        assertExistingIntegrationStatus(status)
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
    fun `should create a new root folder if no previous integration exists`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        webClientBuilderProvider.mockAccessToken("driveToken")
        stubNewRootFolderRequest()

        val status = whenCalculatingIntegrationStatus()

        assertNewIntegrationStatus(status)
        assertNewIntegration(preconditions.fry)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should create a new root folder if previously recorded is trashed`() {
        givenExistingDriveIntegration(preconditions.fry)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubGetRequestTo("/drive/v3/files/fryFolderId") {
            withQueryParam("fields", equalTo("name, trashed, id"))
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            willReturnOkJson(
                """{
                    "id": "fryFolderId",
                    "trashed": true,
                    "name": "fryFolder"
                }"""
            )
        }
        stubNewRootFolderRequest()

        val status = whenCalculatingIntegrationStatus()

        assertNewIntegrationStatus(status)
        assertNewIntegration(preconditions.fry)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should create a new root folder if previously recorded is not found`() {
        givenExistingDriveIntegration(preconditions.fry)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubGetRequestTo("/drive/v3/files/fryFolderId") {
            withQueryParam("fields", equalTo("name, trashed, id"))
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            willReturn(notFound())
        }
        stubNewRootFolderRequest()

        val status = whenCalculatingIntegrationStatus()

        assertNewIntegrationStatus(status)
        assertNewIntegration(preconditions.fry)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should fail on getting content if OAuth2 client is not authorized`() {
        webClientBuilderProvider.mockAuthorizationFailure()

        assertThatThrownBy {
            whenDownloadingDocumentContent(preconditions.workspace)
        }.isInstanceOf(StorageAuthorizationRequiredException::class.java)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should download file content`() {
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubGetRequestTo("/drive/v3/files/testLocation") {
            withQueryParam("alt", equalTo("media"))
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            willReturnResponse {
                withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                withBody("Test Content".toByteArray())
            }
        }

        val contentBuffers = whenDownloadingDocumentContent(preconditions.workspace)

        assertThat(contentBuffers.consumeToString()).isEqualTo("Test Content")
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should fail on saving content if integration is not configured`() {
        assertThatThrownBy {
            whenSavingDocument(preconditions.workspace)
        }.isInstanceOf(StorageAuthorizationRequiredException::class.java)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should fail on saving content if OAuth2 client is not authorized`() {
        givenExistingDriveIntegration(preconditions.fry)
        webClientBuilderProvider.mockAuthorizationFailure()

        assertThatThrownBy {
            whenSavingDocument(preconditions.workspace)
        }.isInstanceOf(StorageAuthorizationRequiredException::class.java)
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should successfully upload a new document`() {
        givenExistingDriveIntegration(preconditions.fry)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubGetWorkspaceFolder(
            preconditions.workspace,
            """{
                "files": [{
                    "id": "fryWorkspaceFolderId",
                    "name": "fryWorkspaceFolder"
                }]
            }"""
        )
        stubNewFileUpload("fryWorkspaceFolderId")

        val documentResponse = whenSavingDocument(preconditions.workspace)

        assertThat(documentResponse).isEqualTo(
            SaveDocumentResponse(
                storageLocation = "newFryFileId",
                sizeInBytes = 42
            )
        )
    }

    @Test
    @WithSaMockUser("Fry")
    fun `should create a workspace folder if not exists during document upload`() {
        givenExistingDriveIntegration(preconditions.fry)
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubGetWorkspaceFolder(preconditions.workspace, """{ "files": [] } """)
        stubPostRequestTo("/drive/v3/files") {
            withQueryParam("fields", equalTo("id, name"))
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            withRequestBody(
                equalToJson(
                    """{
                        "name": "${preconditions.workspace.id}",
                        "mimeType": "application/vnd.google-apps.folder",
                        "parents": ["fryFolderId"]
                    }"""
                )
            )
            willReturnOkJson(
                """{ 
                    "id": "workspaceFolderId", 
                    "name": "${preconditions.workspace.id}"
                }"""
            )
        }
        stubNewFileUpload("workspaceFolderId")

        val documentResponse = whenSavingDocument(preconditions.workspace)

        assertThat(documentResponse).isEqualTo(
            SaveDocumentResponse(
                storageLocation = "newFryFileId",
                sizeInBytes = 42
            )
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `should send push notification with new auth URL on authorization failure`() {
        clientAuthorizationProvider.stub {
            onBlocking { buildAuthorizationUrl(eq("google-drive"), any()) } doReturn "authUrl"
        }

        GlobalScope.run {
            applicationEventPublisher.publishEvent(
                OAuth2FailedEvent(
                    user = preconditions.fry,
                    context = coroutineContext,
                    clientRegistrationId = OAUTH2_CLIENT_REGISTRATION_ID
                )
            )
        }

        await().untilAsserted {
            verifyBlocking(pushNotificationService) {
                sendPushNotification(
                    userId = preconditions.fry.id!!,
                    eventName = AUTH_EVENT_NAME,
                    data = GoogleDriveStorageIntegrationStatus(
                        folderId = null,
                        folderName = null,
                        authorizationRequired = true,
                        authorizationUrl = "authUrl"
                    )
                )
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `should process success authorization event`() {
        webClientBuilderProvider.mockAccessToken("driveToken")
        stubNewRootFolderRequest()

        GlobalScope.run {
            applicationEventPublisher.publishEvent(
                OAuth2SucceededEvent(
                    user = preconditions.fry,
                    context = coroutineContext,
                    clientRegistrationId = OAUTH2_CLIENT_REGISTRATION_ID
                )
            )
        }

        await().untilAsserted {
            verifyBlocking(pushNotificationService) {
                sendPushNotification(
                    userId = preconditions.fry.id!!,
                    eventName = AUTH_EVENT_NAME,
                    data = GoogleDriveStorageIntegrationStatus(
                        folderId = "newFolderId",
                        folderName = "newFolder",
                        authorizationRequired = false,
                        authorizationUrl = null
                    )
                )
            }
            assertNewIntegration(preconditions.fry)
        }
    }

    private fun stubExistingRootFolder() {
        stubGetRequestTo("/drive/v3/files/fryFolderId") {
            withQueryParam("fields", equalTo("name, trashed, id"))
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            willReturnOkJson(
                """{
                    "id": "fryFolderId",
                    "trashed": false,
                    "name": "fryFolder"
                }"""
            )
        }
    }

    private fun stubGetWorkspaceFolder(workspace: Workspace, responseJson: String) {
        stubGetRequestTo("/drive/v3/files") {
            withQueryParam(
                "q",
                equalTo("'fryFolderId' in parents and name = '${workspace.id}' and trashed = false")
            )
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            willReturn(okJson(responseJson))
        }
    }

    private fun stubNewFileUpload(parent: String) {
        stubPostRequestTo("/upload/drive/v3/files") {
            withQueryParam("fields", equalTo("id, size"))
            withQueryParam("uploadType", equalTo("multipart"))
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            withHeader(HttpHeaders.CONTENT_TYPE, matching("${MediaType.MULTIPART_FORM_DATA_VALUE}.*"))
            withMultipartRequestBody(
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
            withMultipartRequestBody(
                aMultipart("media").withBody(equalTo("Document Body"))
            )
            willReturnOkJson(
                """{
                    "id": "newFryFileId",
                    "size": 42
                }"""
            )
        }
    }

    private fun whenDownloadingDocumentContent(workspace: Workspace): Flow<DataBuffer> {
        return runBlocking {
            documentsStorage.getDocumentContent(workspace, "testLocation")
        }
    }

    private fun whenSavingDocument(workspace: Workspace): SaveDocumentResponse {
        val documentBody = ByteArrayInputStream("Document Body".toByteArray())
        return runBlocking {
            documentsStorage.saveDocument(
                SaveDocumentRequest(
                    fileName = "testFileName",
                    workspace = workspace,
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
        stubPostRequestTo("/drive/v3/files") {
            withQueryParam("fields", equalTo("id, name"))
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer driveToken"))
            withRequestBody(
                equalToJson(
                    """{
                        "name": "simple-accounting",
                        "mimeType": "application/vnd.google-apps.folder",
                        "parents": []
                    }"""
                )
            )
            willReturnOkJson(
                """{
                    "id": "newFolderId",
                    "name": "newFolder"
                }"""
            )
        }
    }

    private fun whenCalculatingIntegrationStatus() =
        runBlocking { documentsStorage.getCurrentUserIntegrationStatus() }

    private fun assertNewIntegration(fry: PlatformUser) {
        assertThat(jdbcAggregateTemplate.findAll(GoogleDriveStorageIntegration::class.java))
            .singleElement().satisfies(Consumer { integration ->
                assertThat(integration.userId).isEqualTo(fry.id!!)
                assertThat(integration.folderId).isEqualTo("newFolderId")
            })
    }

    private fun givenExistingDriveIntegration(fry: PlatformUser) {
        jdbcAggregateTemplate.save(
            GoogleDriveStorageIntegration(
                userId = fry.id!!,
                folderId = "fryFolderId"
            )
        )
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }
}
