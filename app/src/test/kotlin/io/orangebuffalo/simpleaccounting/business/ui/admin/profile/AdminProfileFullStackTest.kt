package io.orangebuffalo.simpleaccounting.business.ui.admin.profile

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

/**
 * Tests My Profile page rendering for admin users (no document storage section for admins).
 * See also:
 * - [io.orangebuffalo.simpleaccounting.business.ui.user.profile.UserProfileFullStackTest] for regular user profile rendering
 * - [io.orangebuffalo.simpleaccounting.business.ui.shared.profile.PasswordChangeFullStackTest] for password change functionality
 * - [io.orangebuffalo.simpleaccounting.business.ui.shared.profile.LanguagePreferencesFullStackTest] for language and locale preferences
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

        page.withBlockedGqlApiResponse(
            "userProfile",
            initiator = {
                page.openMyProfilePage { }
            },
            blockedRequestSpec = {
                page.shouldBeMyProfilePage {
                    reportRendering("my-profile.admin-loading-state")
                }
            }
        )

        page.shouldBeMyProfilePage {
            shouldHaveDocumentsStorageSectionHidden()
            shouldHaveLanguagePreferencesSectionVisible()
            shouldHavePasswordChangeSectionVisible()
        }
    }
}
