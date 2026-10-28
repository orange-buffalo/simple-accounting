package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.IntegrationsPage.Companion.openIntegrationsPage
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.Companion.shouldBeWiseSetupPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withFailedGqlApiResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IntegrationsFullStackTest : SaFullStackTestBase() {
    @Test
    fun `should show inactive status when automatic imports are disabled`(page: Page) {
        val fry = preconditions { fry().also {
            wiseIntegrationSettings(workspace = workspace(owner = it), fetchTransactionsPeriodically = false)
        } }
        page.authenticateViaCookie(fry)
        page.openIntegrationsPage {
            status.shouldBeRegular("inactive — no automatic sync")
            reportRendering("integrations.inactive")
        }
        WiseApiMocks.shouldHaveNoRequests()
    }

    @Test
    fun `should cancel integration removal without deleting settings or statements`(page: Page) {
        val data = preconditions { object {
            val fry = fry()
            val settings = wiseIntegrationSettings(workspace = workspace(owner = fry))
            val statement = accountStatementRecord(integration = settings)
        } }
        page.authenticateViaCookie(data.fry)
        page.openIntegrationsPage {
            remove.click()
            confirmation.shouldBeVisible()
            reportRenderingWithPopovers("integrations.remove-confirmation")
            confirmation.clickButton("Keep integration")
            remove.shouldBeEnabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(data.settings)
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.statement)
        WiseApiMocks.shouldHaveNoRequests()
    }

    @Test
    fun `should remove the integration and its statements and offer setup again`(page: Page) {
        val fry = preconditions { fry().also {
            val settings = wiseIntegrationSettings(workspace = workspace(owner = it), authenticationRequired = true)
            accountStatementRecord(integration = settings)
        } }
        page.authenticateViaCookie(fry)
        page.openIntegrationsPage {
            remove.click()
            confirmation.shouldBeVisible()
            page.withBlockedGqlApiResponse(
                queryOrMutationName = "removeWiseIntegration",
                initiator = { confirmation.clickButton("Remove integration") },
                blockedRequestSpec = {
                    remove.shouldBeDisabled()
                    providers.shouldHaveUpdateDisabled()
                    reportRendering("integrations.removing")
                },
            )
            providers.setupWise()
        }
        page.shouldBeWiseSetupPage { tokenStep.shouldBeOpen() }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        WiseApiMocks.shouldHaveNoRequests()
    }

    @Test
    fun `should retain integration data and permit retry after removal fails`(page: Page) {
        val data = preconditions { object {
            val fry = fry()
            val settings = wiseIntegrationSettings(workspace = workspace(owner = fry))
            val statement = accountStatementRecord(integration = settings)
        } }
        page.authenticateViaCookie(data.fry)
        page.openIntegrationsPage {
            page.withFailedGqlApiResponse(operationName = "removeWiseIntegration") {
                remove.click()
                confirmation.shouldBeVisible().clickButton("Remove integration")
                shouldHaveNotifications { error() }
                remove.shouldBeEnabled()
                status.shouldBeSuccess("active")
            }
            remove.click()
            confirmation.shouldBeVisible().clickButton("Keep integration")
            remove.shouldBeEnabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(data.settings)
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.statement)
        WiseApiMocks.shouldHaveNoRequests()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should prioritize authentication failures and allow updating Wise`(automaticImportsEnabled: Boolean, page: Page) {
        val fry = preconditions {
            fry().also { user ->
                wiseIntegrationSettings(workspace = workspace(owner = user), token = WiseApiMocks.TOKEN,
                    authenticationRequired = true, fetchTransactionsPeriodically = automaticImportsEnabled)
            }
        }
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(fry)
        page.openIntegrationsPage {
            status.shouldBeError("Requires auth")
            authenticationTooltip.shouldHaveTextOnFocus("The latest automatic import encountered authorization issues. Verify your Wise access and save the integration again to resume automatic imports.")
            reportRenderingWithPopovers("integrations.authentication-required-$automaticImportsEnabled")
            providers.updateWise()
        }
        page.shouldBeWiseSetupPage {
            accountsStep.shouldBeOpen()
            back.shouldBeEnabled()
        }
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
        page.shouldBeWiseSetupPage {
            accountsStep.shouldBeOpen()
            back.shouldBeEnabled()
        }
    }
}
