package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.tests.infra.database.LegacyPreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class AdminProfileFullStackTest(
    preconditionsFactory: LegacyPreconditionsFactory,
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
