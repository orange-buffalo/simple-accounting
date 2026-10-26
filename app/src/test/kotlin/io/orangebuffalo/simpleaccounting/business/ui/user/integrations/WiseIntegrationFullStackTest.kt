package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.Companion.openWiseSetupPage
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.AccountData
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

class WiseIntegrationFullStackTest : SaFullStackTestBase() {
    private val normalAccounts = arrayOf(
        AccountData(profile = "Philip J. Fry", profileType = "Personal Account", currency = "USD", amount = "USD\u00a01,234.56"),
        AccountData(profile = "Philip J. Fry", profileType = "Personal Account", currency = "EUR", amount = "EUR\u00a00.00"),
        AccountData(profile = "Planet Express", profileType = "Business Account", currency = "GBP", amount = "-GBP\u00a012.34"),
    )
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    @Test
    fun `should format account balances using the user locale`(page: Page) {
        val fry = preconditions {
            platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = "uk_UA", language = "en")).withWorkspace()
        }
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(fry)
        page.openWiseSetupPage {
            token { input.fill(WiseApiMocks.TOKEN) }
            next.click()
            accounts.shouldHaveExactData(
                normalAccounts[0].copy(amount = "1\u00a0234,56\u00a0USD"),
                normalAccounts[1].copy(amount = "0,00\u00a0EUR"),
                normalAccounts[2].copy(amount = "-12,34\u00a0GBP"),
            )
            reportRendering("integrations.wise-accounts-uk-locale")
        }
    }

    @Test
    fun `should create integration selecting accounts independently across currencies and profiles`(page: Page) {
        WiseApiMocks.setupAccountsWithSharedIds()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            tokenStep.shouldBeOpen()
            tokenStep.shouldBeNonInteractive()
            accountsStep.shouldBeNonInteractive()
            finishStep.shouldBeNonInteractive()
            back.shouldBeHidden()
            next.shouldBeDisabled()
            token { input { fill(WiseApiMocks.TOKEN); shouldHaveAttribute(name = "type", value = "password") } }
            reportRendering("integrations.wise-token")
            page.withBlockedGqlApiResponse(queryOrMutationName = "listWiseAccounts", initiator = { next.click() }, blockedRequestSpec = {
                status.shouldBeRegular("Verifying token and loading accounts…")
                accountsStep.shouldBeOpen()
                back.shouldBeDisabled()
                next.shouldBeDisabled()
                reportRendering("integrations.wise-loading-accounts")
            })
            accounts.shouldHaveExactData(
                *normalAccounts,
            )
            accountsStep.shouldBeOpen()
            tokenStep.shouldBeCompleted()
            back.shouldBeEnabled()
            back.click()
            tokenStep.shouldBeOpen()
            back.shouldBeHidden()
            token { input.shouldHaveValue(WiseApiMocks.TOKEN) }
            next.click()
            accounts.shouldHaveExactData(*normalAccounts)
            configuration.fetchTransactions.shouldBeSwitchedOn()
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
                normalAccounts[1].copy(selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"),
            )
            reportRendering("integrations.wise-accounts")
            configuration.fetchTransactions.toggle()
            configuration.fetchTransactions.shouldBeSwitchedOff()
            next.click()
            status.shouldBeSuccess("Wise integration saved successfully.")
            finishStep.shouldBeOpen()
            finishStep.shouldBeCompleted()
            back.shouldBeEnabled()
            next.shouldBeHidden()
            reportRendering("integrations.wise-success")
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(WiseIntegrationSettings(
            workspaceId = preconditions.workspace.id!!, token = WiseApiMocks.TOKEN,
            fetchTransactionsPeriodically = false, authenticationRequired = false, lastSuccessfulFetchTime = null,
            accounts = setOf(
                WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD"),
                WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "EUR"),
                WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP"),
            ),
        ))
    }

    @Test
    fun `should update accounts of the active provider using the saved credential`(page: Page) {
        val settings = preconditions {
            wiseIntegrationSettings(
                workspace = preconditions.workspace,
                token = WiseApiMocks.TOKEN,
                accounts = setOf(
                    WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD"),
                    WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "EUR"),
                    WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP"),
                ),
                fetchTransactionsPeriodically = false,
                authenticationRequired = true,
            )
        }
        WiseApiMocks.setupAccountsWithSharedIds()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            accounts.shouldHaveExactData(
                normalAccounts[0].copy(selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[1].copy(selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"),
            )
            accountsStep.shouldBeOpen()
            tokenStep.shouldBeCompleted()
            configuration.fetchTransactions.shouldBeSwitchedOff()
            configuration.fetchTransactions.toggle()
            configuration.fetchTransactions.shouldBeSwitchedOn()
            reportRendering("integrations.wise-configuration")
            accounts.account("USD").click()
            next.click()
            status.shouldBeSuccess("Wise integration saved successfully.")
        }
        val updated = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        updated.id.shouldBe(settings.id)
        updated.token.shouldBe(WiseApiMocks.TOKEN)
        updated.fetchTransactionsPeriodically.shouldBe(true)
        updated.authenticationRequired.shouldBe(false)
        updated.accounts.shouldContainExactly(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "EUR"), WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP"))
    }

    @Test
    fun `should mark deleted saved accounts and exclude them when saving`(page: Page) {
        preconditions {
            wiseIntegrationSettings(
                workspace = preconditions.workspace,
                token = WiseApiMocks.TOKEN,
                accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR"), WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP")),
            )
        }
        WiseApiMocks.setupAccounts()
        WiseApiMocks.removeEuroBalance()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            val deletedBalance = AccountData(
                profile = "Philip J. Fry", profileType = "Personal Account", currency = "EUR",
                amount = null, deleted = true,
            )
            accounts.shouldHaveExactData(normalAccounts[0], deletedBalance, normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"))
            accounts.account("EUR").shouldBeDeleted()
            accountsStep.shouldBeOpen()
            tokenStep.shouldBeCompleted()
            reportRendering("integrations.wise-update-deleted")
            next.click()
            status.shouldBeSuccess("Wise integration saved successfully.")
            back.click()
            accounts.shouldHaveExactData(normalAccounts[0], normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"))
        }
        val settings = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        settings.token.shouldBe(WiseApiMocks.TOKEN)
        settings.accounts.shouldContainExactly(WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP"))
        settings.fetchTransactionsPeriodically.shouldBe(true)
        settings.authenticationRequired.shouldBe(false)
    }

    @Test
    fun `should show unavailable saved profiles without inventing profile details`(page: Page) {
        preconditions {
            wiseIntegrationSettings(
                workspace = preconditions.workspace,
                token = WiseApiMocks.TOKEN,
                accounts = setOf(WiseIntegrationAccount(profileId = 999, accountId = 701, currency = "JPY")),
            )
        }
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            accounts.shouldHaveExactData(
                *normalAccounts,
                AccountData(profile = "Profile ID: 999", profileType = null, currency = "JPY", amount = null, deleted = true),
            )
            accounts.account("JPY").shouldBeDeleted()
            next.shouldBeDisabled()
            reportRendering("integrations.wise-unavailable-profile")
        }
    }

    @Test
    fun `should revisit token step to replace the saved credential`(page: Page) {
        preconditions {
            wiseIntegrationSettings(
                workspace = preconditions.workspace,
                token = WiseApiMocks.TOKEN,
                accounts = setOf(WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP")),
            )
        }
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            accounts.shouldHaveExactData(normalAccounts[0], normalAccounts[1], normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"))
            back.click()
            tokenStep.shouldBeOpen()
            token { input.shouldHaveValue("") }
            reportRendering("integrations.wise-update-token")
            next.click()
            accounts.shouldHaveExactData(normalAccounts[0], normalAccounts[1], normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"))
            back.click()
            val replacementToken = "bender-replacement-token"
            WiseApiMocks.setupAccounts(token = replacementToken)
            token { input.fill(replacementToken) }
            tokenStep.shouldBePending()
            back.shouldBeHidden()
            next.click()
            accounts.shouldHaveExactData(normalAccounts[0], normalAccounts[1], normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"))
            tokenStep.shouldBeCompleted()
            accounts.account("USD").click()
            page.withBlockedGqlApiResponse(queryOrMutationName = "saveWiseIntegrationSettings", initiator = { next.click() }, blockedRequestSpec = {
                status.shouldBeRegular("Saving Wise integration…")
                finishStep.shouldBeOpen()
                back.shouldBeDisabled()
                next.shouldBeHidden()
                reportRendering("integrations.wise-saving")
            })
            status.shouldBeSuccess("Wise integration saved successfully.")
            back.click()
            accounts.shouldHaveExactData(
                normalAccounts[0].copy(selected = true, selectionLabel = "✓ Selected"),
                normalAccounts[1],
                normalAccounts[2].copy(selected = true, selectionLabel = "✓ Selected"),
            )
            accountsStep.shouldBeCompleted()
        }
        val settings = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        settings.token.shouldBe("bender-replacement-token")
        settings.accounts.shouldContainExactly(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD"), WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP"))
        settings.fetchTransactionsPeriodically.shouldBe(true)
        settings.authenticationRequired.shouldBe(false)
    }

    @Test
    fun `should recover from account listing outages without saving`(page: Page) {
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
            next.shouldBeDisabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
    }

    @Test
    fun `should recover when the token is rejected during save without persisting`(page: Page) {
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            token { input.fill(WiseApiMocks.TOKEN) }
            next.click()
            accounts.shouldHaveExactData(*normalAccounts)
            accounts.account("EUR").click()
            WiseApiMocks.rejectToken()
            next.click()
            status.shouldBeError("Wise rejected this token. Check the token and try again.")
            finishStep.shouldBeOpen()
            next.shouldBeHidden()
            back.click()
            tokenStep.shouldBeOpen()
            back.shouldBeHidden()
            token { input.shouldHaveValue(WiseApiMocks.TOKEN) }
            WiseApiMocks.setupAccounts()
            next.click()
            accounts.shouldHaveExactData(*normalAccounts)
            next.shouldBeDisabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
    }

    @Test
    fun `should recover when selected accounts disappear during save without persisting`(page: Page) {
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            token { input.fill(WiseApiMocks.TOKEN) }
            next.click()
            accounts.shouldHaveExactData(*normalAccounts)
            accounts.account("EUR").click()
            WiseApiMocks.removeEuroBalance()
            next.click()
            status.shouldBeError("The selected accounts have changed. Go back and select your accounts again.")
            finishStep.shouldBeOpen()
            next.shouldBeHidden()
            back.click()
            accountsStep.shouldBeOpen()
            accounts.shouldHaveExactData(normalAccounts[0], normalAccounts[2])
            next.shouldBeDisabled()
            accounts.account("USD").click()
            next.shouldBeEnabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
    }

    @Test
    fun `should recover from save outages without saving`(page: Page) {
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            token { input.fill(WiseApiMocks.TOKEN) }
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
    fun `should recover from a rejected token`(page: Page) {
        WiseApiMocks.rejectToken()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            token { input.fill("bender-revoked-token") }
            next.click()
            status.shouldBeError("Wise rejected this token. Check the token and try again.")
            reportRendering("integrations.wise-invalid-token")
            back.click()
            token { input.fill(WiseApiMocks.TOKEN) }
            WiseApiMocks.setupAccounts()
            next.click()
            accounts.shouldHaveExactData(*normalAccounts)
            next.shouldBeDisabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
    }

    @Test
    fun `should report empty accounts`(page: Page) {
        WiseApiMocks.noAccounts()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            token { input.fill(WiseApiMocks.TOKEN) }
            next.click()
            status.shouldBeRegular("No currency balances are available. Add an account in Wise, then try again.")
            accounts.shouldHaveExactData()
            next.shouldBeDisabled()
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
    }
}
