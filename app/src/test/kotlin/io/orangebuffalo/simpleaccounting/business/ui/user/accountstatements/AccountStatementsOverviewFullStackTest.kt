package io.orangebuffalo.simpleaccounting.business.ui.user.accountstatements

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.integrations.StatementType
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.accountstatements.AccountStatementsOverviewPage.Companion.openAccountStatementsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.accountstatements.AccountStatementsOverviewPage.Companion.shouldBeAccountStatementsOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class AccountStatementsOverviewFullStackTest : SaFullStackTestBase() {
    @Test
    fun `should display transactions and expand all available information`(page: Page) {
        val data = preconditions {
            object {
                val fry = fry()
                val integration = wiseIntegrationSettings(workspace = workspace(owner = fry))
                val conversion = accountStatementRecord(
                    integration = integration, externalId = "Slurm-conversion", partyName = "Philip J. Fry",
                    type = StatementType.CONVERSION, occurredAt = Instant.parse("3025-01-15T12:00:00Z"),
                    amountInCents = 1200, currency = "USD", feesInCents = 0, feesCurrency = "USD",
                    runningBalanceInCents = -125, runningBalanceCurrency = "USD",
                    originalAmountInCents = 1000, originalCurrency = "EUR", sourceAmountInCents = 1000,
                    sourceCurrency = "EUR", targetAmountInCents = 1200, targetCurrency = "USD",
                    exchangeRate = BigDecimal("1.234567"), additionalData = "Description: Slurm supplies\nReference: Planet Express",
                )
                val credit = accountStatementRecord(integration = integration, externalId = "Moon-delivery", type = StatementType.CREDIT, occurredAt = Instant.parse("3025-01-13T12:00:00Z"))
                init {
                    accountStatementRecord(integration = integration, externalId = "Robot-oil", partyName = "Bender", occurredAt = Instant.parse("3025-01-14T12:00:00Z"))
                    accountStatementRecord(integration = integration, externalId = "Unknown-Slurm", type = StatementType.OTHER, occurredAt = Instant.parse("3025-01-12T12:00:00Z"))
                    accountStatementRecord()
                }
            }
        }
        page.authenticateViaCookie(data.fry)
        page.withBlockedGqlApiResponse(
            "accountStatementsPage",
            initiator = { page.openAccountStatementsOverviewPage() },
            blockedRequestSpec = {
                page.shouldBeAccountStatementsOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("account-statements.loading")
                }
            },
        )
        page.shouldBeAccountStatementsOverviewPage {
            pageItems.shouldHaveExactData(
                summary(title = "Philip J. Fry", day = 15, type = "Conversion", amount = "USD 12.00"),
                summary(title = "Bender", day = 14, type = "Debit", amount = "USD 1.25"),
                summary(title = "Moon-delivery", day = 13, type = "Credit", amount = "USD 1.25"),
                summary(title = "Unknown-Slurm", day = 12, type = "Other", amount = "USD 1.25"),
            )
            reportRendering("account-statements.collapsed")
            statement("Philip J. Fry").shouldHaveDetails(
                sections = arrayOf(
                    DetailsSectionSpec(title = "Transaction details", attributes = listOf(
                        "Party" to "Philip J. Fry", "Amount" to "USD 12.00", "Transaction type" to "Conversion",
                        "Transaction date" to "15 Jan 3025, 11:00 pm", "Source integration" to "Wise",
                        "Account ID" to "301", "Account currency" to "USD", "External transaction ID" to "Slurm-conversion",
                        "Fees" to "USD 0.00", "Running balance" to "-USD 1.25", "Original amount" to "EUR 10.00",
                        "Conversion source amount" to "EUR 10.00", "Conversion target amount" to "USD 12.00",
                        "Exchange rate" to "1.234567", "Imported at" to "29 Mar 1999, 9:01 am",
                        "Integration ID" to data.integration.id!!, "Record ID" to data.conversion.id!!,
                    )),
                    DetailsSectionSpec(title = "Additional information", content = "Description: Slurm supplies\nReference: Planet Express"),
                ),
            )
            reportRendering("account-statements.expanded")
            statement("Moon-delivery").shouldHaveDetails(
                sections = arrayOf(DetailsSectionSpec(title = "Transaction details", attributes = listOf(
                    "Amount" to "USD 1.25", "Transaction type" to "Credit", "Transaction date" to "13 Jan 3025, 11:00 pm",
                    "Source integration" to "Wise", "Account ID" to "301", "Account currency" to "USD",
                    "External transaction ID" to "Moon-delivery", "Imported at" to "29 Mar 1999, 9:01 am",
                    "Integration ID" to data.integration.id!!,
                    "Record ID" to data.credit.id!!,
                ))),
            )
        }
    }

    @Test
    fun `should filter transactions and show empty results`(page: Page) {
        val data = preconditions {
            fry().also { user ->
                val integration = wiseIntegrationSettings(workspace = workspace(owner = user))
                accountStatementRecord(integration = integration, externalId = "Slurm", partyName = "Fry", occurredAt = Instant.parse("3025-01-15T12:00:00Z"), additionalData = "Reference: Mars")
                accountStatementRecord(integration = integration, externalId = "Robot-oil", partyName = "Bender", occurredAt = Instant.parse("3025-01-14T12:00:00Z"))
            }
        }
        page.authenticateViaCookie(data)
        page.openAccountStatementsOverviewPage {
            pageItems.shouldHaveTitles("Fry", "Bender")
            filters.addSelectFilter(label = "Source integration", value = "Wise").closeByOutsideClick()
            filters.setDateRange(label = "Transaction dates (UTC)", from = "3025-01-15", to = "3025-01-15").closeByOutsideClick()
            filters.applySearchWithEnter("mars")
            pageItems.shouldHaveTitles("Fry")
            reportRendering("account-statements.filtered")
            filters.applySearchWithEnter("Omicron")
            pageItems.shouldHaveTitles()
            reportRendering("account-statements.empty")
            filters.clearAll()
            pageItems.shouldHaveTitles("Fry", "Bender")
        }
    }

    private fun summary(title: String, day: Int, type: String, amount: String) = SaOverviewItemData(
        title = title,
        primaryAttributes = listOf(
            primaryAttribute(icon = SaIconType.CALENDAR, text = "$day Jan 3025, 11:00 pm"),
            primaryAttribute(icon = SaIconType.INTEGRATIONS, text = "Wise · USD"),
        ),
        middleColumnContent = type,
        lastColumnContent = amount,
    )
}
