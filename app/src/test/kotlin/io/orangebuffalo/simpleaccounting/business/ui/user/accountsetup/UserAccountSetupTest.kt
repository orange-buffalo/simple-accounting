package io.orangebuffalo.simpleaccounting.business.ui.user.accountsetup

import com.microsoft.playwright.Page
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.loginAs
import io.orangebuffalo.simpleaccounting.business.ui.user.accountsetup.AccountSetupPage.Companion.shouldBeAccountSetupPage
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenuHidden
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test

class UserAccountSetupTest : SaFullStackTestBase() {

    @Test
    fun `should setup new account successfully`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldHaveSideMenuHidden()
        page.shouldBeAccountSetupPage {
            workspaceName { input.fill("Workspace X") }
            defaultCurrency {
                input.shouldHaveValue("AUD")
                input.fill("USD")
            }
            completeSetupButton { click() }
        }
        page.shouldBeDashboardPage()
        page.shouldHaveSideMenu()
            .shouldHaveWorkspaceName("Workspace X")

        aggregateTemplate.findSingle<Workspace>().should { workspace ->
            workspace.name.shouldBe("Workspace X")
            workspace.defaultCurrency.shouldBe("USD")
            workspace.ownerId.shouldBe(preconditions.fry.id)
        }
    }

    /**
     * Verifies integration of API errors with UI. Full set of validation cases is tested on the API level.
     */
    @Test
    fun `should validate inputs`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldBeAccountSetupPage {
            workspaceName { input.fill("") }
            defaultCurrency { input.fill("") }
            completeSetupButton { click() }
            shouldHaveNotifications { validationFailed() }
            workspaceName { shouldHaveValidationError("This value is required and should not be blank") }
            defaultCurrency { shouldHaveValidationError("This value is required and should not be blank") }
            workspaceName { input.fill("x".repeat(256)) }
            defaultCurrency { input.fill("x".repeat(4)) }
            completeSetupButton { click() }
            shouldHaveNotifications { validationFailed() }
            workspaceName { shouldHaveValidationError("The length of this value should be no longer than 255 characters") }
            defaultCurrency { shouldHaveValidationError("The length of this value should be no longer than 3 characters") }
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            // no workspace yet - setup is required
        }
    }
}
