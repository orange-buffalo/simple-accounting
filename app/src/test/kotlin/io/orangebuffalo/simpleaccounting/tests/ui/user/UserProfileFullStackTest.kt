package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.tests.infra.database.LegacyPreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class UserProfileFullStackTest(
    preconditionsFactory: LegacyPreconditionsFactory,
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

            // TODO #1628: workspace should not be required?
            val workspace = workspace(owner = fry)
        }
    }
}
