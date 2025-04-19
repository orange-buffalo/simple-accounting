package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleDriveApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthRecordedRequest
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.DocumentStorageSection.DocumentStorageConfig
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.DocumentStorageSection.GoogleDriveSettings
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.OAuthAuthorizationPopup.Companion.setupErrorIdForOAuthAuthorizationFailure
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.OAuthAuthorizationPopup.Companion.shouldHaveAuthorizationPopupOpenBy
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.shouldBeMyProfilePage
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate

@SimpleAccountingFullStackTest
class UserProfileGoogleDriveDocumentStorageFullStackTest(
    preconditionsFactory: PreconditionsFactory,
    @Autowired private val aggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val tokenGenerator: TokenGenerator,
) {
    @Test
    fun `should enable Google Drive storage if was not previously configured`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.scruffy) {
        withClue("Google Drive should be turned off") {
            switch.shouldBeSwitchedOff()
            settings.shouldBeHidden()
        }

        withClue("Should show loading indicator when turned on") {
            page.withBlockedApiResponse(
                "**/status",
                initiator = {
                    switch.toggle()
                    settings.status.shouldBeRegular("Verifying integration status...")
                },
                blockedRequestSpec = {
                    assertRendering("profile/documents-storage/google/loading-status")
                }
            )
        }

        withClue("Should save settings") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.scruffy.id!!)
                .documentsStorage.shouldBe("google-drive")
        }

        withClue("Should have unauthorized status until configured") {
            settings {
                shouldBeVisible()
                status.shouldBePending("Authorization required")
            }
            assertRendering("profile/documents-storage/google/authorization-required")
        }
    }

    @Test
    fun `should show current status when GDrive is enabled`(page: Page) =
        page.onGoogleDriveSection(preconditions.calculon) {
            switch.shouldBeSwitchedOn()
            settings.shouldBeVisible()
            settings.status.shouldBePending("Authorization required")
        }

    @Test
    fun `should fail on OAuth authorization if GDrive fails`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.calculon) {
        settings.status.shouldBePending("Authorization required")

        // we do not configure GDrive mock, so request fails with 404
        tokenGenerator.setupErrorIdForOAuthAuthorizationFailure()
        val oauthPopup = withClue("Should initiate authorization flow when requested") {
            page.shouldHaveAuthorizationPopupOpenBy {
                settings.startAuthorizationButton.click()
            }
        }

        withClue("Should fail if GDrive does not reply successfully") {
            oauthPopup.shouldHaveErrorState()
        }

        withClue("Google Drive panel should provide authorization in progress status") {
            settings.status.shouldBeRegular("Authorization in progress...")
            assertRendering("profile/documents-storage/google/authorizing-in-progress")
        }
    }

    @Test
    fun `should create new root folder if not created before`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.scruffy) {
        switch.toggle()
        settings.status.shouldBePending("Authorization required")

        GoogleDriveApiMocks.mockCreateFolder(
            requestName = "simple-accounting",
            responseId = "test-created-folder-id",
            requestParents = emptyList(),
            expectedAuthToken = GoogleOAuthApiMocks.enqueueAccessToken(),
        )

        page.shouldHaveAuthorizationPopupOpenBy {
            settings.startAuthorizationButton.click()
        }

        withClue("Should have success status with the correct folder name") {
            settings {
                shouldBeVisible()
                status.shouldBeSuccess("Google Drive integration is active")
            }
            assertRendering("profile/documents-storage/google/success-created-folder")
        }

        withClue("Should store the created folder in database") {
            val integration = aggregateTemplate.findAll<GoogleDriveStorageIntegration>()
                .firstOrNull { it.userId == preconditions.scruffy.id!! }
                .shouldNotBeNull()
            integration.folderId.shouldBe("test-created-folder-id")
        }

        withClue("Should issue proper OAuth requests") {
            GoogleOAuthApiMocks.recordedRequests()
                .shouldContainExactlyInAnyOrder(
                    GoogleOAuthRecordedRequest.Authorize,
                    GoogleOAuthRecordedRequest.Token,
                )
        }
    }

//    @Test
//    fun `should successfully reauthorize GDrive when folder exists but auth was missing`(
//        page: Page
//    ) = page.onGoogleDriveSection(preconditions.bender) {
//        settings.status.shouldBePending("Authorization required")
//
//        // Configure GDrive API mock to return existing folder
//        ThirdPartyApisMocks.server.stubFor(
//            get(urlPathMatching("/google-drive-mocks/drive/v3/files/existing-folder-id"))
//                .withQueryParam("fields", equalTo("name, trashed, id"))
//                .willReturn(
//                    aResponse()
//                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                        .withBody(
//                            buildJsonObject {
//                                put("id", "existing-folder-id")
//                                put("name", "simple-accounting")
//                                put("trashed", false)
//                            }.toString()
//                        )
//                )
//        )
//
//        val oauthPopup = withClue("Should initiate authorization flow when requested") {
//            page.shouldHaveAuthorizationPopupOpenBy {
//                settings.startAuthorizationButton.click()
//            }
//        }
//
//        withClue("Should complete OAuth flow successfully with existing folder") {
//            // Complete the OAuth flow in the popup
//            oauthPopup.page.navigate(oauthPopup.page.url() + "&code=test-authorization-code&state=test-state")
//
//            // Wait for the popup to show success
//            oauthPopup.page.waitForSelector(".oauth-callback-page .sa-status-label--success", new Page.WaitForSelectorOptions().setState("visible"))
//
//            // Wait for the integration status to update with existing folder info
//            settings.status.shouldBeSuccess("Connected to Google Drive")
//
//            // Verify the integration details with existing folder are shown
//            settings.assertRendering("profile/documents-storage/google/successful-integration")
//        }
//    }

    private fun Page.onGoogleDriveSection(
        user: PlatformUser,
        spec: DocumentStorageConfig<GoogleDriveSettings>.() -> Unit
    ) {
        loginAs(user)
        shouldHaveSideMenu().clickMyProfile()
        val page = shouldBeMyProfilePage()
        page.shouldHaveDocumentsStorageSectionVisible {
            shouldHaveGoogleDriveConfigVisible {
                spec(this)
            }
        }
    }

    private val preconditions by preconditionsFactory {
        object {
            val scruffy = platformUser(
                userName = "scruffy",
                documentsStorage = null,
            )

            val calculon = platformUser(
                userName = "calculon",
                documentsStorage = "google-drive",
            )

            init {
                workspace(owner = scruffy)
                workspace(owner = calculon)
            }
        }
    }
}
