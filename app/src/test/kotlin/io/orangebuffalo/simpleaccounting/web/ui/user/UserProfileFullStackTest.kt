package io.orangebuffalo.simpleaccounting.web.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class UserProfileFullStackTest(
    preconditionsFactory: PreconditionsFactory,
) {

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        page.loginAs(preconditions.fry)

        page.shouldHaveSideMenu().clickMyProfile()

        page.shouldBeMyProfilePage()
            .shouldHaveDocumentsStorageSectionVisible()
            .shouldHaveLanguagePreferencesSectionVisible()
            .shouldHavePasswordChangeSectionVisible()
    }

    private val preconditions by preconditionsFactory {
        object {
            val fry = fry()

            // TODO #23: workspace should not be required?
            val workspace = workspace(owner = fry)
        }
    }
}
