package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.IntegrationsPage.Companion.openIntegrationsPage
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.Companion.shouldBeWiseSetupPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import org.junit.jupiter.api.Test

class IntegrationsFullStackTest : SaFullStackTestBase() {
    @Test
    fun `should show authentication failures instead of active status and allow updating Wise`(page: Page) {
        val fry = preconditions {
            fry().also { user ->
                wiseIntegrationSettings(workspace = workspace(owner = user), token = WiseApiMocks.TOKEN, authenticationRequired = true)
            }
        }
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(fry)
        page.openIntegrationsPage {
            status.shouldBeError("Authentication required. Verify your Wise access and save the integration again to resume transaction fetching.")
            reportRendering("integrations.authentication-required")
            providers.updateWise()
        }
        page.shouldBeWiseSetupPage { accountsStep.shouldBeOpen() }
    }

    @Test
    fun `should open Wise setup from the provider card`(page: Page) {
        val fry = preconditions { fry().withWorkspace() }
        page.authenticateViaCookie(fry)
        page.openIntegrationsPage {
            reportRendering("integrations.providers")
            providers.setupWise()
        }
        page.shouldBeWiseSetupPage { tokenStep.shouldBeOpen() }
    }

    @Test
    fun `should open active Wise integration from the provider card`(page: Page) {
        val fry = preconditions {
            fry().also { user ->
                val workspace = workspace(owner = user)
                wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN)
            }
        }
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(fry)
        page.openIntegrationsPage {
            status.shouldBeSuccess("active")
            reportRendering("integrations.active")
            providers.updateWise()
        }
        page.shouldBeWiseSetupPage { accountsStep.shouldBeOpen() }
    }
}
