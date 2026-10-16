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
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.AccountData
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

class WiseIntegrationFullStackTest : SaFullStackTestBase() {
    private val normalAccounts = arrayOf(
        AccountData(profile = "Philip J. Fry", currency = "USD", name = "Currency balance", accountLabel = "Account ID: 301"),
        AccountData(profile = "Philip J. Fry", currency = "EUR", name = "Slurm fund", accountLabel = "Account ID: 302"),
        AccountData(profile = "Planet Express", currency = "GBP", name = "Currency balance", accountLabel = "Account ID: 401"),
    )
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    @Test
    fun `should select accounts independently across currencies and profiles and update the active provider`(page: Page) {
        WiseApiMocks.setupAccountsWithSharedIds()
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
                reportRendering("integrations.wise-loading-accounts")
            })
            accounts.shouldHaveExactData(
                AccountData(profile = "Philip J. Fry", currency = "USD", name = "Currency balance", accountLabel = "Account ID: 301"),
                AccountData(profile = "Philip J. Fry", currency = "EUR", name = "Currency balance", accountLabel = "Account ID: 301"),
                AccountData(profile = "Planet Express", currency = "GBP", name = "Currency balance", accountLabel = "Account ID: 401"),
            )
            val dollars = accounts.account("USD")
            val euros = accounts.account("EUR")
            dollars.shouldBeUnselected()
            euros.shouldBeUnselected()
            next.shouldBeDisabled()
            dollars.click()
            dollars.shouldBeSelected()
            euros.shouldBeUnselected()
            next.shouldBeEnabled()
            dollars.click()
            next.shouldBeDisabled()
            euros.click()
            dollars.shouldBeUnselected()
            euros.shouldBeSelected()
            dollars.click()
            dollars.shouldBeSelected()
            euros.shouldBeSelected()
            accounts.account("GBP").click()
            accounts.shouldHaveExactData(
                normalAccounts[0].copy(selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[1].copy(name = "Currency balance", accountLabel = "Account ID: 301", selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"),
            )
            reportRendering("integrations.wise-accounts")
            next.click()
            status.shouldBeSuccess("Wise integration saved successfully.")
            reportRendering("integrations.wise-success")
        }
        val settings = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        settings.workspaceId.shouldBe(preconditions.workspace.id)
        settings.token.shouldBe(WiseApiMocks.TOKEN)
        settings.accounts.shouldContainExactly(
            WiseIntegrationAccount(101, 301, "USD"),
            WiseIntegrationAccount(101, 301, "EUR"),
            WiseIntegrationAccount(202, 401, "GBP"),
        )
        page.openIntegrationsPage {
            status.shouldBeSuccess("active")
            reportRendering("integrations.active")
            providers.updateWise()
        }
        page.shouldBeWiseSetupPage {
            accounts.shouldHaveExactData(
                normalAccounts[0].copy(selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[1].copy(name = "Currency balance", accountLabel = "Account ID: 301", selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"),
            )
            accountsStep.shouldBeOpen()
            tokenStep.shouldBeCompleted()
            accounts.account("USD").click()
            next.click()
            status.shouldBeSuccess("Wise integration saved successfully.")
        }
        val updated = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        updated.id.shouldBe(settings.id)
        updated.token.shouldBe(WiseApiMocks.TOKEN)
        updated.accounts.shouldContainExactly(WiseIntegrationAccount(101, 301, "EUR"), WiseIntegrationAccount(202, 401, "GBP"))
    }

    @Test
    fun `should mark deleted saved accounts and revisit token step to replace the credential`(page: Page) {
        preconditions {
            wiseIntegrationSettings(
                workspace = preconditions.workspace,
                token = WiseApiMocks.TOKEN,
                accounts = setOf(WiseIntegrationAccount(101, 302, "EUR"), WiseIntegrationAccount(202, 401, "GBP")),
            )
        }
        WiseApiMocks.setupAccounts()
        WiseApiMocks.removeJar()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            val deletedJar = AccountData(
                profile = "Profile ID: 101", currency = "EUR", name = "Currency balance",
                accountLabel = "Account ID: 302", deleted = true,
            )
            accounts.shouldHaveExactData(normalAccounts[0], normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"), deletedJar)
            accounts.account("EUR").shouldBeDeleted()
            accountsStep.shouldBeOpen()
            tokenStep.shouldBeCompleted()
            reportRendering("integrations.wise-update-deleted")
            tokenStep.click()
            tokenStep.shouldBeOpen()
            token { input.shouldHaveValue("") }
            reportRendering("integrations.wise-update-token")
            accountsStep.click()
            accounts.shouldHaveExactData(normalAccounts[0], normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"), deletedJar)
            tokenStep.click()
            val replacementToken = "bender-replacement-token"
            WiseApiMocks.setupAccounts(token = replacementToken)
            WiseApiMocks.removeJar(token = replacementToken)
            token { input.fill(replacementToken) }
            tokenStep.shouldBePending()
            accountsStep.shouldBeDisabled()
            next.click()
            accounts.shouldHaveExactData(normalAccounts[0], normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"), deletedJar)
            tokenStep.shouldBeCompleted()
            accounts.account("USD").click()
            page.withBlockedGqlApiResponse("saveWiseIntegration", initiator = { next.click() }, blockedRequestSpec = {
                status.shouldBeRegular("Saving Wise integration…")
                tokenStep.shouldBeDisabled()
                accountsStep.shouldBeDisabled()
                reportRendering("integrations.wise-saving")
            })
            status.shouldBeSuccess("Wise integration saved successfully.")
            accountsStep.click()
            accounts.shouldHaveExactData(
                normalAccounts[0].copy(selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"),
            )
            accountsStep.shouldBeCompleted()
        }
        val settings = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        settings.token.shouldBe("bender-replacement-token")
        settings.accounts.shouldContainExactly(WiseIntegrationAccount(101, 301, "USD"), WiseIntegrationAccount(202, 401, "GBP"))
    }

    @Test
    fun `should recover from Wise outages without saving`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        WiseApiMocks.rejectToken(503)
        page.openWiseSetupPage {
            token { input.fill(WiseApiMocks.TOKEN) }
            next.click()
            status.shouldBeError("Wise could not be reached or its accounts could not be loaded. Please try again.")
            back.click()
            WiseApiMocks.setupAccounts()
            next.click()
            accounts.shouldHaveExactData(*normalAccounts)
            accounts.account("EUR").click()
            WiseApiMocks.failBalances()
            next.click()
            status.shouldBeError("Wise could not be reached or its accounts could not be loaded. Please try again.")
            reportRendering("integrations.wise-unavailable")
            WiseApiMocks.setupAccounts()
            back.click()
            accounts.shouldHaveExactData(*normalAccounts)
            next.shouldBeDisabled()
            accounts.account("EUR").click()
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
            accounts.shouldHaveExactData()
            next.shouldBeDisabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
    }
}
