package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.Companion.shouldBeMyProfilePage
import org.junit.jupiter.api.Test

/**
 * Tests My Profile page rendering for admin users (no document storage section for admins).
 * See also:
 * - [io.orangebuffalo.simpleaccounting.tests.ui.user.UserProfileFullStackTest] for regular user profile rendering
 * - [io.orangebuffalo.simpleaccounting.tests.ui.shared.PasswordChangeFullStackTest] for password change functionality
 * - [io.orangebuffalo.simpleaccounting.tests.ui.shared.LanguagePreferencesFullStackTest] for language and locale preferences
 */
class AdminProfileFullStackTest : SaFullStackTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val admin = farnsworth()
        }
    }

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        page.authenticateViaCookie(preconditions.admin)
        
        // Capture initial loading state while profile data is being fetched
        page.withBlockedApiResponse(
            "profile*",
            initiator = {
                page.openMyProfilePage {}
            },
            blockedRequestSpec = {
                // Report rendering on body during loading (container may not exist yet if API is required)
                page.locator("body").reportRendering("profile.admin.initial-loading")
            }
        )
        
        page.shouldBeMyProfilePage {
            shouldHaveDocumentsStorageSectionHidden()
            shouldHaveLanguagePreferencesSectionVisible()
            shouldHavePasswordChangeSectionVisible()
            reportRendering("profile.admin.initial-state")
        }
    }
}
