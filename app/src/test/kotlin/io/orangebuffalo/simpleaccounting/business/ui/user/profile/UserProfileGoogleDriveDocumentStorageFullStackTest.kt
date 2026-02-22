package io.orangebuffalo.simpleaccounting.business.ui.user.profile

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.DocumentStorageSection.DocumentStorageConfig
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.DocumentStorageSection.GoogleDriveSettings
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.OAuthAuthorizationPopup.Companion.shouldHaveAuthorizationPopupOpenBy
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleDriveApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.OAuthRecordedRequest
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import org.junit.jupiter.api.Test

/**
 * Tests Google Drive document storage integration on My Profile page.
 * See also:
 * - [UserProfileFullStackTest] for basic My Profile page rendering
 * - [io.orangebuffalo.simpleaccounting.business.ui.shared.profile.PasswordChangeFullStackTest] for password change functionality
 * - [io.orangebuffalo.simpleaccounting.business.ui.shared.profile.LanguagePreferencesFullStackTest] for language and locale preferences
 */
class UserProfileGoogleDriveDocumentStorageFullStackTest : SaFullStackTestBase() {
    @Test
    fun `should enable Google Drive storage if was not previously configured`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.scruffy) {
        withHint("Google Drive should be turned off") {
            switch.shouldBeSwitchedOff()
            settings.shouldBeHidden()
        }

        withHint("Should show loading indicator when turned on") {
            page.withBlockedApiResponse(
                "**/status",
                initiator = {
                    switch.toggle()
                },
                blockedRequestSpec = {
                    settings.status.shouldBeRegular("Verifying integration status...")
                    reportRendering("profile.documents-storage.google.loading-status")
                }
            )
        }

        withHint("Should save settings") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.scruffy.id!!)
                .documentsStorage.shouldBe("google-drive")
        }

        withHint("Should have unauthorized status until configured") {
            settings.shouldBeVisible()
            assertAuthorizationRequiredStatus()
            reportRendering("profile.documents-storage.google.authorization-required")
        }
    }

    @Test
    fun `should show current status when GDrive is enabled`(page: Page) =
        page.onGoogleDriveSection(preconditions.calculon) {
            switch.shouldBeSwitchedOn()
            settings.shouldBeVisible()
            assertAuthorizationRequiredStatus()
        }

    @Test
    fun `should disable Google Drive storage if was previously configured`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.calculon) {
        withHint("Google Drive should be turned on") {
            switch.shouldBeSwitchedOn()
            settings.shouldBeVisible()
        }

        switch.toggle()
        settings.shouldBeHidden()

        shouldEventually("Should save settings") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.calculon.id!!)
                .documentsStorage.shouldBeNull()
        }
    }

    @Test
    fun `should fail on OAuth authorization if GDrive fails`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.calculon) {
        assertAuthorizationRequiredStatus()

        // we do not configure GDrive mock, so request fails with 404
        val oauthPopup = withHint("Should initiate authorization flow when requested") {
            page.shouldHaveAuthorizationPopupOpenBy {
                settings.startAuthorizationButton.click()
            }
        }

        withHint("Should fail if GDrive does not reply successfully") {
            oauthPopup.shouldHaveErrorState()
        }

        withHint("Google Drive panel should provide authorization in progress status") {
            settings.status.shouldBeRegular("Authorization in progress...")
            reportRendering("profile.documents-storage.google.authorizing-in-progress")
        }
    }

    @Test
    fun `should create new root folder if not created before`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.scruffy) {
        switch.toggle()
        assertAuthorizationRequiredStatus()

        GoogleDriveApiMocks.mockCreateFolder(
            requestName = "simple-accounting",
            responseId = "test-created-folder-id",
            requestParents = emptyList(),
            expectedAuthToken = GoogleOAuthMocks.token().enqueue(),
        )

        startAuthorization(page)

        withHint("Should have success status with the correct folder name") {
            settings {
                shouldBeVisible()
                status.shouldBeSuccess("Google Drive integration is active")
            }
            reportRendering("profile.documents-storage.google.success-created-folder")
        }
        preconditions.scruffy.assertIntegrationFolderId("test-created-folder-id")
        assertOauthRequests(
            OAuthRecordedRequest.Authorize,
            OAuthRecordedRequest.TokenByCode,
        )
    }

    @Test
    fun `should successfully reauthorize GDrive when folder exists but auth was missing`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.yivo) {
        assertAuthorizationRequiredStatus()

        GoogleDriveApiMocks.mockFindFile(
            fileId = "previously-created-folder-id",
            fileName = "previously-created-folder-name",
            expectedAuthToken = GoogleOAuthMocks.token().enqueue(),
        )

        startAuthorization(page)

        assertSuccessStatus("previously-created-folder-name")
        preconditions.yivo.assertIntegrationFolderId("previously-created-folder-id")
        assertOauthRequests(
            OAuthRecordedRequest.Authorize,
            OAuthRecordedRequest.TokenByCode,
        )
    }

    @Test
    fun `should re-create the folder upon authorization if trashed`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.yivo) {
        assertAuthorizationRequiredStatus()

        val authToken = GoogleOAuthMocks.token().enqueue(expectedRequestsCount = 2)
        GoogleDriveApiMocks.mockFindFile(
            fileId = "previously-created-folder-id",
            fileName = "previously-created-folder-name",
            expectedAuthToken = authToken,
            trashed = true,
        )
        GoogleDriveApiMocks.mockCreateFolder(
            requestName = "simple-accounting",
            responseId = "new-folder-id",
            requestParents = emptyList(),
            expectedAuthToken = authToken,
        )

        startAuthorization(page)

        assertSuccessStatus("simple-accounting")
        preconditions.yivo.assertIntegrationFolderId("new-folder-id")
        assertOauthRequests(
            OAuthRecordedRequest.Authorize,
            OAuthRecordedRequest.TokenByCode,
        )
        assertPreviousRootFolderIsRequestedFromGoogleDrive()
    }

    @Test
    fun `should re-create the folder upon authorization if not found`(
        page: Page
    ) = page.onGoogleDriveSection(preconditions.yivo) {
        assertAuthorizationRequiredStatus()

        val expectedAuthToken = GoogleOAuthMocks.token().enqueue(expectedRequestsCount = 2)
        GoogleDriveApiMocks.mockFindFile(
            fileId = "previously-created-folder-id",
            fileName = "previously-created-folder-name",
            expectedAuthToken = expectedAuthToken,
            send404Response = true,
        )
        GoogleDriveApiMocks.mockCreateFolder(
            requestName = "simple-accounting",
            responseId = "new-folder-id",
            requestParents = emptyList(),
            expectedAuthToken = expectedAuthToken,
        )

        startAuthorization(page)

        assertSuccessStatus("simple-accounting")
        preconditions.yivo.assertIntegrationFolderId("new-folder-id")
        assertOauthRequests(
            OAuthRecordedRequest.Authorize,
            OAuthRecordedRequest.TokenByCode,
        )
        assertPreviousRootFolderIsRequestedFromGoogleDrive()
    }

    @Test
    fun `should provide success status if auth configured before`(
        page: Page
    ) {
        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(preconditions.yivo)
        GoogleDriveApiMocks.mockFindFile(
            fileId = "previously-created-folder-id",
            fileName = "existing-root-folder",
            expectedAuthToken = accessToken,
        )
        page.onGoogleDriveSection(preconditions.yivo) {
            assertSuccessStatus("existing-root-folder")
            preconditions.yivo.assertIntegrationFolderId("previously-created-folder-id")
            assertNoOauthInteractions()
            assertPreviousRootFolderIsRequestedFromGoogleDrive()
        }
    }

    @Test
    fun `should create root folder if not created before but auth configured`(
        page: Page
    ) {
        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(preconditions.yivo)
        GoogleDriveApiMocks.mockCreateFolder(
            requestName = "simple-accounting",
            responseId = "new-folder-id",
            requestParents = emptyList(),
            expectedAuthToken = accessToken,
        )
        page.onGoogleDriveSection(preconditions.yivo) {
            assertSuccessStatus("simple-accounting")
            preconditions.yivo.assertIntegrationFolderId("new-folder-id")
            assertNoOauthInteractions()
        }
    }

    @Test
    fun `should re-create root folder if deleted and auth configured before`(
        page: Page
    ) {
        val accessToken = GoogleOAuthMocks.token()
            .enqueue(expectedRequestsCount = 2)
            .persist(preconditions.yivo)
        GoogleDriveApiMocks.mockFindFile(
            fileId = "previously-created-folder-id",
            fileName = "previously-created-folder-name",
            expectedAuthToken = accessToken,
            trashed = true,
        )
        GoogleDriveApiMocks.mockCreateFolder(
            requestName = "simple-accounting",
            responseId = "new-folder-id",
            requestParents = emptyList(),
            expectedAuthToken = accessToken,
        )
        page.onGoogleDriveSection(preconditions.yivo) {
            assertSuccessStatus("simple-accounting")
            preconditions.yivo.assertIntegrationFolderId("new-folder-id")
            assertNoOauthInteractions()
            assertPreviousRootFolderIsRequestedFromGoogleDrive()
        }
    }

    @Test
    fun `should re-create root folder if not found by ID and auth configured before`(
        page: Page
    ) {
        val accessToken = GoogleOAuthMocks.token()
            .enqueue(expectedRequestsCount = 2)
            .persist(preconditions.yivo)
        GoogleDriveApiMocks.mockFindFile(
            fileId = "previously-created-folder-id",
            fileName = "previously-created-folder-name",
            expectedAuthToken = accessToken,
            send404Response = true,
        )
        GoogleDriveApiMocks.mockCreateFolder(
            requestName = "simple-accounting",
            responseId = "new-folder-id",
            requestParents = emptyList(),
            expectedAuthToken = accessToken,
        )
        page.onGoogleDriveSection(preconditions.yivo) {
            assertSuccessStatus("simple-accounting")
            preconditions.yivo.assertIntegrationFolderId("new-folder-id")
            assertNoOauthInteractions()
            assertPreviousRootFolderIsRequestedFromGoogleDrive()
        }
    }

    @Test
    fun `should issue a new token and use it if previous token expired and refresh token persisted`(
        page: Page
    ) {
        // previously created token is expired
        GoogleOAuthMocks.token("old-token")
            .persist(
                user = preconditions.yivo,
                expired = true,
                refreshToken = "refresh-token-x",
            )
        // new token is issued
        val newAccessToken = GoogleOAuthMocks.token("new-token").enqueue()

        GoogleDriveApiMocks.mockFindFile(
            fileId = "previously-created-folder-id",
            fileName = "existing-root-folder",
            expectedAuthToken = newAccessToken,
        )
        page.onGoogleDriveSection(preconditions.yivo) {
            assertSuccessStatus("existing-root-folder")
            preconditions.yivo.assertIntegrationFolderId("previously-created-folder-id")
            assertOauthRequests(
                OAuthRecordedRequest.TokenByRefreshToken("refresh-token-x"),
            )
            assertPreviousRootFolderIsRequestedFromGoogleDrive()
        }
    }

    private fun DocumentStorageConfig<GoogleDriveSettings>.assertAuthorizationRequiredStatus() {
        withHint("Should have authorization required status") {
            settings {
                shouldBeVisible()
                status.shouldBePending("Authorization required")
            }
        }
    }

    private fun DocumentStorageConfig<GoogleDriveSettings>.startAuthorization(
        page: Page,
    ) = page.shouldHaveAuthorizationPopupOpenBy {
        settings.startAuthorizationButton.click()
    }

    private fun DocumentStorageConfig<GoogleDriveSettings>.assertSuccessStatus(
        folderName: String,
    ) {
        withHint("Should have success status with the correct folder name") {
            settings {
                shouldBeVisible()
                status.shouldBeSuccess("Google Drive integration is active")
                detailsMessage.shouldHaveText("All documents are stored in $folderName folder")
            }
        }
    }

    private fun PlatformUser.assertIntegrationFolderId(
        folderId: String,
    ) {
        withHint("Should update the database state") {
            val integration = this.shouldHaveSingleDriveIntegration()
            integration.folderId.shouldBe(folderId)
        }
    }

    private fun assertOauthRequests(vararg expectedRequests: OAuthRecordedRequest) {
        withHint("Should issue proper OAuth requests") {
            GoogleOAuthMocks.recordedRequests()
                .shouldContainExactlyInAnyOrder(*expectedRequests)
        }
    }

    private fun assertNoOauthInteractions() {
        withHint("Should not issue any OAuth requests") {
            GoogleOAuthMocks.recordedRequests().shouldBeEmpty()
        }
    }

    private fun assertPreviousRootFolderIsRequestedFromGoogleDrive() {
        withHint("Should try find previously stored file") {
            GoogleDriveApiMocks.verifyFindFileRequest(fileId = "previously-created-folder-id")
        }
    }

    private fun Page.onGoogleDriveSection(
        user: PlatformUser,
        spec: DocumentStorageConfig<GoogleDriveSettings>.() -> Unit
    ) {
        authenticateViaCookie(user)
        openMyProfilePage {
            shouldHaveDocumentsStorageSectionVisible {
                shouldHaveGoogleDriveConfigVisible {
                    spec(this)
                }
            }
        }
    }

    private fun PlatformUser.shouldHaveSingleDriveIntegration(): GoogleDriveStorageIntegration {
        return aggregateTemplate.findAll<GoogleDriveStorageIntegration>()
            .firstOrNull { it.userId == this.id!! }
            .shouldNotBeNull()
    }

    private val preconditions by lazyPreconditions {
        object {
            val scruffy = platformUser(
                userName = "scruffy",
                documentsStorage = null,
            ).withWorkspace()

            val calculon = platformUser(
                userName = "calculon",
                documentsStorage = "google-drive",
            ).withWorkspace()

            val yivo = platformUser(
                userName = "yivo",
                documentsStorage = "google-drive",
            ).withWorkspace().also {
                save(
                    GoogleDriveStorageIntegration(
                        userId = it.id!!,
                        folderId = "previously-created-folder-id",
                    )
                )
            }
        }
    }
}
