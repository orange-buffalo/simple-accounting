package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.Companion.shouldBeMyProfilePage
import org.junit.jupiter.api.Test

/**
 * Tests basic rendering of My Profile page for regular users.
 * See also:
 * - [io.orangebuffalo.simpleaccounting.tests.ui.shared.PasswordChangeFullStackTest] for password change functionality
 * - [UserProfileGoogleDriveDocumentStorageFullStackTest] for Google Drive storage integration
 * - [io.orangebuffalo.simpleaccounting.tests.ui.shared.LanguagePreferencesFullStackTest] for language and locale preferences
 * - [io.orangebuffalo.simpleaccounting.tests.ui.admin.AdminProfileFullStackTest] for admin profile specifics
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
