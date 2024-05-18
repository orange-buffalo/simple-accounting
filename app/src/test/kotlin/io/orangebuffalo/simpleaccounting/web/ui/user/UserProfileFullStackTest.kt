package io.orangebuffalo.simpleaccounting.web.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SimpleAccountingFullStackTest
class UserProfileFullStackTest(
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        val preconditions = setupPreconditions()
        page.loginAs(preconditions.fry)

        page.shouldHaveSideMenu().clickMyProfile()

        page.shouldBeMyProfilePage()
            .shouldHaveDocumentsStorageSectionVisible()
            .shouldHaveLanguagePreferencesSectionVisible()
            .shouldHavePasswordChangeSectionVisible()
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = fry()

        // TODO #23: workspace should not be required?
        val workspace = workspace(owner = fry)
    }
}
