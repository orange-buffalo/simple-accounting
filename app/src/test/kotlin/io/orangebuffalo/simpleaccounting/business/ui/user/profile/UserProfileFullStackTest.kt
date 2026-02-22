package io.orangebuffalo.simpleaccounting.business.ui.user.profile

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

/**
 * Tests basic rendering of My Profile page for regular users.
 * See also:
 * - [io.orangebuffalo.simpleaccounting.business.ui.shared.profile.PasswordChangeFullStackTest] for password change functionality
 * - [UserProfileGoogleDriveDocumentStorageFullStackTest] for Google Drive storage integration
 * - [io.orangebuffalo.simpleaccounting.business.ui.shared.profile.LanguagePreferencesFullStackTest] for language and locale preferences
 * - [io.orangebuffalo.simpleaccounting.business.ui.admin.profile.AdminProfileFullStackTest] for admin profile specifics
 */
class UserProfileFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)

        page.withBlockedGqlApiResponse(
            "userProfile",
            initiator = {
                page.openMyProfilePage { }
            },
            blockedRequestSpec = {
                page.shouldBeMyProfilePage {
                    reportRendering("my-profile.loading-state")
                }
            }
        )

        page.shouldBeMyProfilePage {
            shouldHaveDocumentsStorageSectionVisible()
            shouldHaveLanguagePreferencesSectionVisible()
            shouldHavePasswordChangeSectionVisible()
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
        }
    }
}
