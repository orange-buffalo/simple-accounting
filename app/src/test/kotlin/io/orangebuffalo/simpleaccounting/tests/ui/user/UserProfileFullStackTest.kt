package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.openMyProfilePage
import org.junit.jupiter.api.Test

class UserProfileFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage()
            .shouldHaveDocumentsStorageSectionVisible()
            .shouldHaveLanguagePreferencesSectionVisible()
            .shouldHavePasswordChangeSectionVisible()
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()

            // TODO #1628: workspace should not be required?
            val workspace = workspace(owner = fry)
        }
    }
}
