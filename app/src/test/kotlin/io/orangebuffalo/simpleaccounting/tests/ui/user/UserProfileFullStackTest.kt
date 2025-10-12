package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.Companion.shouldBeMyProfilePage
import org.junit.jupiter.api.Test

/**
 * Tests basic rendering of My Profile page for regular users.
 * See also:
 * - [PasswordChangeFullStackTest] for password change functionality
 * - [UserProfileGoogleDriveDocumentStorageFullStackTest] for Google Drive storage integration
 * - [LanguagePreferencesFullStackTest] for language and locale preferences
 * - [io.orangebuffalo.simpleaccounting.tests.ui.admin.AdminProfileFullStackTest] for admin profile specifics
 */
class UserProfileFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        
        // Capture initial loading state while profile data is being fetched
        page.withBlockedApiResponse(
            "profile*",
            initiator = {
                page.openMyProfilePage {}
            },
            blockedRequestSpec = {
                // Report rendering on body during loading (container may not exist yet if API is required)
                page.locator("body").reportRendering("profile.user.initial-loading")
            }
        )
        
        page.shouldBeMyProfilePage {
            shouldHaveDocumentsStorageSectionVisible()
            shouldHaveLanguagePreferencesSectionVisible()
            shouldHavePasswordChangeSectionVisible()
            reportRendering("profile.user.initial-state")
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
        }
    }
}
