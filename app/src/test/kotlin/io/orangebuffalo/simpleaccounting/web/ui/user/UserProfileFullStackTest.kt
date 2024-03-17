package io.orangebuffalo.simpleaccounting.web.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class UserProfileFullStackTest {

    @Test
    fun `should render My Profile page with proper sections`(testData: UserProfileTestData, page: Page) {
        page.loginAs(testData.fry)

        page.shouldHaveSideMenu().clickMyProfile()

        page.shouldBeMyProfilePage()
            .shouldHaveDocumentsStorageSectionVisible()
            .shouldHaveLanguagePreferencesSectionVisible()
            .shouldHavePasswordChangeSectionVisible()
    }

    class UserProfileTestData : TestDataDeprecated {
        val fry = Prototypes.fry()

        // TODO #23: workspace should not be required?
        val workspace = Prototypes.workspace(owner = fry)
    }
}
