package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import org.junit.jupiter.api.Test

class UserProfileFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        page.loginAs(preconditions.fry)

        page.shouldHaveSideMenu().clickMyProfile()

        page.shouldBeMyProfilePage()
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
