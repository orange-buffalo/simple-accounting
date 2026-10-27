package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.IntegrationsPage.Companion.openIntegrationsPage
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.Companion.shouldBeWiseSetupPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

class IntegrationsFullStackTest : SaFullStackTestBase() {
    @Test
    fun `should disable automatic imports without verifying the saved token`(page: Page) {
        val data = preconditions { object {
            val fry = fry()
            val settings = wiseIntegrationSettings(
                workspace = workspace(owner = fry), authenticationRequired = true, lastSuccessfulFetchTime = MOCK_TIME,
                accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD", lastSuccessfulFetchTime = MOCK_TIME)),
            )
            val statement = accountStatementRecord(integration = settings)
        } }
        page.authenticateViaCookie(data.fry)
        page.openIntegrationsPage {
            page.withBlockedGqlApiResponse(queryOrMutationName = "setWiseAutomaticFetching", initiator = { disable.click() }, blockedRequestSpec = {
                disable.shouldBeDisabled()
                remove.shouldBeDisabled()
                reportRendering("integrations.disabling")
            })
            enable.shouldBeEnabled()
            status.shouldBeRegular("Automatic imports disabled")
            reportRendering("integrations.disabled")
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            .shouldBeEntityWithFields(data.settings.copy(fetchTransactionsPeriodically = false))
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.statement)
        WiseApiMocks.shouldHaveNoRequests()
    }

    @Test
    fun `should enable imports without clearing authentication required`(page: Page) {
        val data = preconditions { object {
            val fry = fry()
            val settings = wiseIntegrationSettings(workspace = workspace(owner = fry), fetchTransactionsPeriodically = false, authenticationRequired = true)
        } }
        page.authenticateViaCookie(data.fry)
        page.openIntegrationsPage {
            status.shouldBeRegular("Automatic imports disabled")
            enable.click()
            disable.shouldBeEnabled()
            status.shouldBeError("Authentication required. Verify your Wise access and save the integration again to resume transaction fetching.")
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            .shouldBeEntityWithFields(data.settings.copy(fetchTransactionsPeriodically = true))
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
            disable.shouldBeEnabled()
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
            confirmation.shouldBeVisible().clickButton("Remove integration")
            providers.setupWise()
        }
        page.shouldBeWiseSetupPage { tokenStep.shouldBeOpen() }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        WiseApiMocks.shouldHaveNoRequests()
    }

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
