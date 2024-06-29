package io.orangebuffalo.simpleaccounting.web.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class AdminProfileFullStackTest(
    preconditionsFactory: PreconditionsFactory,
) {
    private val preconditions by preconditionsFactory {
        object {
            val admin = farnsworth()
        }
    }

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        page.loginAs(preconditions.admin)

        page.shouldHaveSideMenu().clickMyProfile()

        page.shouldBeMyProfilePage()
            .shouldHaveDocumentsStorageSectionHidden()
            .shouldHaveLanguagePreferencesSectionVisible()
            .shouldHavePasswordChangeSectionVisible()
    }
}
