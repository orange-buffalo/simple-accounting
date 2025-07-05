package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.openMyProfilePage
import org.junit.jupiter.api.Test

class AdminProfileFullStackTest : SaFullStackTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val admin = farnsworth()
        }
    }

    @Test
    fun `should render My Profile page with proper sections`(page: Page) {
        page.authenticateViaCookie(preconditions.admin)
        page.openMyProfilePage()
            .shouldHaveDocumentsStorageSectionHidden()
            .shouldHaveLanguagePreferencesSectionVisible()
            .shouldHavePasswordChangeSectionVisible()
    }
}
