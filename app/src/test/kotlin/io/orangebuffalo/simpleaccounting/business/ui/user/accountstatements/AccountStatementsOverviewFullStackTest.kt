package io.orangebuffalo.simpleaccounting.business.ui.user.accountstatements

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.business.integrations.StatementType
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.InvoicesOverviewPage.Companion.shouldBeInvoicesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.accountstatements.AccountStatementsOverviewPage.Companion.openAccountStatementsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.accountstatements.AccountStatementsOverviewPage.Companion.shouldBeAccountStatementsOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import io.orangebuffalo.simpleaccounting.tests.infra.utils.dataValues
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class AccountStatementsOverviewFullStackTest : SaFullStackTestBase() {
    @Test
    fun `should navigate from linked invoice details to prefiltered invoices`(page: Page) {
        val fry = preconditions {
            fry().also { user ->
                val workspace = workspace(owner = user)
                val integration = wiseIntegrationSettings(workspace = workspace)
                val customer = customer(workspace = workspace)
                val invoice = invoice(customer = customer, invoiceNumber = "PE-3025/42 #Mars", amount = 125, dateIssued = LocalDate.of(3025, 1, 15))
                invoice(customer = customer, invoiceNumber = "PE-3025-43", dateIssued = LocalDate.of(3025, 1, 14))
                accountStatementRecord(
                    integration = integration, externalId = "Moon-delivery", type = StatementType.CREDIT,
                    linkedInvoice = invoice,
                )
            }
        }
        page.authenticateViaCookie(fry)
        page.openAccountStatementsOverviewPage {
            pageItems.shouldHaveTitles("Moon-delivery")
            statement("Moon-delivery").shouldHaveDetails(
                sections = arrayOf(DetailsSectionSpec(title = "Transaction details", attributes = listOf(
                    "Amount" to "USD 1.25", "Transaction type" to "Credit", "Transaction date" to "29 Mar 1999, 9:01 am",
                    "Source integration" to "Wise", "Account ID" to "301", "Account currency" to "USD",
                    "External transaction ID" to "Moon-delivery", "Imported at" to "29 Mar 1999, 9:01 am",
                    "Invoice" to "PE-3025/42 #Mars",
                ))),
            )
            reportRendering("account-statements.linked-invoice")
            statement("Moon-delivery").clickDetailsLink("PE-3025/42 #Mars")
        }
        page.shouldBeInvoicesOverviewPage {
            pageItems.shouldHaveTitles("PE-3025/42 #Mars")
            filters.shouldHaveTextFilterValue(label = "Search", value = "PE-3025/42 #Mars").closeByOutsideClick()
            filters.clearAll()
            pageItems.shouldHaveTitles("PE-3025/42 #Mars", "PE-3025-43")
        }
    }

    @Test
    fun `should display transactions and expand all available information`(page: Page) {
        val data = preconditions {
            fry().also { fry ->
                val integration = wiseIntegrationSettings(workspace = workspace(owner = fry))
                accountStatementRecord(
                    integration = integration, externalId = "Slurm-conversion", partyName = "Philip J. Fry",
                    type = StatementType.CONVERSION, occurredAt = Instant.parse("3025-01-15T12:00:00Z"),
                    amountInCents = 1200, currency = "USD", feesInCents = 0, feesCurrency = "USD",
                    runningBalanceInCents = -125, runningBalanceCurrency = "USD",
                    originalAmountInCents = 1000, originalCurrency = "EUR", sourceAmountInCents = 1000,
                    sourceCurrency = "EUR", targetAmountInCents = 1200, targetCurrency = "USD",
                    exchangeRate = BigDecimal("1.234567"), additionalData = "Description: Slurm supplies\nReference: Planet Express",
                )
                accountStatementRecord(integration = integration, externalId = "Moon-delivery", type = StatementType.CREDIT, occurredAt = Instant.parse("3025-01-13T12:00:00Z"))
                accountStatementRecord(integration = integration, externalId = "Robot-oil", partyName = "Bender", occurredAt = Instant.parse("3025-01-14T12:00:00Z"))
                accountStatementRecord(integration = integration, externalId = "Unknown-Slurm", type = StatementType.OTHER, occurredAt = Instant.parse("3025-01-12T12:00:00Z"))
                accountStatementRecord()
            }
        }
        page.authenticateViaCookie(data)
        page.withBlockedGqlApiResponse(
            queryOrMutationName = "accountStatementsPage",
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
                summary(title = "Philip J. Fry", day = 15, type = dataValues(SaStatusLabel.successStatusValue(), "Conversion"), amount = "USD 12.00"),
                summary(title = "Bender", day = 14, type = dataValues(SaStatusLabel.errorStatusValue(), "Debit"), amount = "USD 1.25"),
                summary(title = "Moon-delivery", day = 13, type = dataValues(SaStatusLabel.successStatusValue(), "Credit"), amount = "USD 1.25"),
                summary(title = "Unknown-Slurm", day = 12, type = dataValues(SaStatusLabel.regularStatusValue(), "Other"), amount = "USD 1.25"),
            )
            shouldHaveTypeBadge(title = "Philip J. Fry", status = "success")
            shouldHaveTypeBadge(title = "Bender", status = "failure")
            shouldHaveTypeBadge(title = "Moon-delivery", status = "success")
            shouldHaveTypeBadge(title = "Unknown-Slurm", status = "regular")
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
                ))),
            )
        }
    }

    @Test
    fun `should distinguish incoming outgoing and unknown conversion directions`(page: Page) {
        val fry = preconditions {
            fry().also { user ->
                val integration = wiseIntegrationSettings(workspace = workspace(owner = user))
                accountStatementRecord(
                    integration = integration, externalId = "Slurm-incoming", type = StatementType.CONVERSION,
                    occurredAt = Instant.parse("3025-01-13T12:00:00Z"),
                    sourceCurrency = "EUR", targetCurrency = "USD",
                )
                accountStatementRecord(
                    integration = integration, externalId = "Slurm-outgoing", type = StatementType.CONVERSION,
                    occurredAt = Instant.parse("3025-01-14T12:00:00Z"),
                    sourceCurrency = "USD", targetCurrency = "EUR",
                )
                accountStatementRecord(
                    integration = integration, externalId = "Slurm-unknown", type = StatementType.CONVERSION,
                    occurredAt = Instant.parse("3025-01-15T12:00:00Z"),
                )
            }
        }
        page.authenticateViaCookie(fry)
        page.openAccountStatementsOverviewPage {
            pageItems.shouldHaveDataSatisfying { items ->
                items.map { it.title to it.middleColumnContent }.shouldContainExactly(
                    "Slurm-unknown" to dataValues(SaStatusLabel.regularStatusValue(), "Conversion"),
                    "Slurm-outgoing" to dataValues(SaStatusLabel.errorStatusValue(), "Conversion"),
                    "Slurm-incoming" to dataValues(SaStatusLabel.successStatusValue(), "Conversion"),
                )
            }
            shouldHaveTypeBadge(title = "Slurm-incoming", status = "success")
            shouldHaveTypeBadge(title = "Slurm-outgoing", status = "failure")
            shouldHaveTypeBadge(title = "Slurm-unknown", status = "regular")
            reportRendering("account-statements.conversion-directions")
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
            filters.setDateRange(label = "Transaction dates", from = "3025-01-15", to = "3025-01-15").closeByOutsideClick()
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

    @ParameterizedTest
    @CsvSource(
        "1999-03-28, 1999-03-27T13:00:00Z, 1999-03-28T14:00:00Z",
        "1999-10-31, 1999-10-30T14:00:00Z, 1999-10-31T13:00:00Z",
    )
    fun `should filter whole browser-local days across daylight saving transitions`(
        date: String,
        start: String,
        end: String,
        page: Page,
    ) {
        val startInstant = Instant.parse(start)
        val endInstant = Instant.parse(end)
        val fry = preconditions {
            fry().also { user ->
                val integration = wiseIntegrationSettings(workspace = workspace(owner = user))
                accountStatementRecord(integration = integration, externalId = "Slurm-before", occurredAt = startInstant.minusNanos(1000))
                accountStatementRecord(integration = integration, externalId = "Slurm-start", occurredAt = startInstant)
                accountStatementRecord(integration = integration, externalId = "Slurm-end", occurredAt = endInstant.minusNanos(1000))
                accountStatementRecord(integration = integration, externalId = "Slurm-after", occurredAt = endInstant)
            }
        }
        page.authenticateViaCookie(fry)
        page.openAccountStatementsOverviewPage {
            pageItems.shouldHaveTitles("Slurm-after", "Slurm-end", "Slurm-start", "Slurm-before")
            filters.setDateRange(label = "Transaction dates", from = date, to = date).closeByOutsideClick()
            pageItems.shouldHaveTitles("Slurm-end", "Slurm-start")
            filters.clearAll()
            pageItems.shouldHaveTitles("Slurm-after", "Slurm-end", "Slurm-start", "Slurm-before")
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
