package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.IntegrationsPage.Companion.openIntegrationsPage
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.Companion.openWiseSetupPage
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.Companion.shouldBeWiseSetupPage
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseViewPage.Companion.shouldBeWiseViewPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withFailedGqlApiResponse
import org.junit.jupiter.api.Test

class WiseIntegrationFullStackTest : SaFullStackTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    @Test
    fun `should activate balances and Jars across profiles and view the active provider`(page: Page) {
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(preconditions.fry)
        page.openIntegrationsPage {
            reportRendering("integrations.providers")
            providers.setupWise()
        }
        page.shouldBeWiseSetupPage {
            next.shouldBeDisabled()
            token { input { fill(WiseApiMocks.TOKEN); shouldHaveAttribute("type", "password") } }
            reportRendering("integrations.wise-token")
            page.withBlockedGqlApiResponse("verifyWiseToken", initiator = { next.click() }, blockedRequestSpec = {
                status.shouldBeRegular("Verifying token and loading accounts…")
            })
            accounts.account("Philip J. Fry", "USD", "Currency balance", "301").shouldBeUnselected()
            accounts.account("Philip J. Fry", "EUR", "Slurm fund", "302").shouldBeUnselected()
            accounts.account("Planet Express", "GBP", "Currency balance", "401").shouldBeUnselected()
            next.shouldBeDisabled()
            val jar = accounts.account("Philip J. Fry", "EUR", "Slurm fund", "302")
            jar.click()
            jar.shouldBeSelected()
            next.shouldBeEnabled()
            jar.click()
            jar.shouldBeUnselected()
            next.shouldBeDisabled()
            jar.click()
            accounts.account("Planet Express", "GBP", "Currency balance", "401").click()
            reportRendering("integrations.wise-accounts")
            next.click()
            status.shouldBeSuccess("Wise integration activated successfully.")
            reportRendering("integrations.wise-success")
        }
        val settings = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        settings.workspaceId.shouldBe(preconditions.workspace.id)
        settings.token.shouldBe(WiseApiMocks.TOKEN)
        settings.accounts.shouldContainExactly(WiseIntegrationAccount(101, 302, "EUR"), WiseIntegrationAccount(202, 401, "GBP"))
        page.openIntegrationsPage {
            status.shouldBeSuccess("active")
            reportRendering("integrations.active")
            providers.viewWise()
        }
        page.shouldBeWiseViewPage()
    }

    @Test
    fun `should display provider loading failure and recover from Wise outages without saving`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.withFailedGqlApiResponse("integrationProviders") {
            page.openIntegrationsPage {
                status.shouldBeError("Wise could not be reached or its accounts could not be loaded. Please try again.")
                reportRendering("integrations.providers-unavailable")
            }
        }
        WiseApiMocks.rejectToken(503)
        page.openWiseSetupPage {
            token { input.fill(WiseApiMocks.TOKEN) }
            next.click()
            status.shouldBeError("Wise could not be reached or its accounts could not be loaded. Please try again.")
            back.click()
            WiseApiMocks.setupAccounts()
            next.click()
            accounts.account("Philip J. Fry", "EUR", "Slurm fund", "302").click()
            WiseApiMocks.failBalances()
            next.click()
            status.shouldBeError("Wise could not be reached or its accounts could not be loaded. Please try again.")
            reportRendering("integrations.wise-unavailable")
            WiseApiMocks.setupAccounts()
            back.click()
            accounts.account("Philip J. Fry", "EUR", "Slurm fund", "302").shouldBeUnselected()
            next.shouldBeDisabled()
            accounts.account("Philip J. Fry", "EUR", "Slurm fund", "302").click()
            next.shouldBeEnabled()
            reportRendering("integrations.wise-recovered-accounts")
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
    }

    @Test
    fun `should recover from rejected token and report empty accounts`(page: Page) {
        WiseApiMocks.rejectToken()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            token { input.fill("bender-revoked-token") }
            next.click()
            status.shouldBeError("Wise rejected this token. Check the token and try again.")
            reportRendering("integrations.wise-invalid-token")
            back.click()
            token { input.fill(WiseApiMocks.TOKEN) }
            WiseApiMocks.noAccounts()
            next.click()
            status.shouldBeRegular("No balances or Jars are available. Add an account in Wise, then try again.")
            next.shouldBeDisabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
    }
}
