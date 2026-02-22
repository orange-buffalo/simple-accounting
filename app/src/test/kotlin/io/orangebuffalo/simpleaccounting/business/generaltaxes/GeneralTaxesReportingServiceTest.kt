package io.orangebuffalo.simpleaccounting.business.generaltaxes

import assertk.assertThat
import assertk.assertions.containsOnly
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class GeneralTaxesReportingServiceTest(
    @Autowired private val taxReportingService: GeneralTaxesReportingService,
) : SaIntegrationTestBase() {

    @Test
    fun `should calculate general tax report`() {
        val actualReport = runBlocking {
            taxReportingService.getGeneralTaxReport(
                preconditions.dateFrom,
                preconditions.dateTo,
                preconditions.planetExpress
            )
        }

        assertThat(actualReport.finalizedCollectedTaxes).containsOnly(
            FinalizedGeneralTaxSummaryItem(
                tax = preconditions.generalTax.id!!,
                includedItemsNumber = 1,
                includedItemsAmount = 400,
                taxAmount = 76
            ),
            FinalizedGeneralTaxSummaryItem(
                tax = preconditions.paidTax1.id!!,
                includedItemsNumber = 2,
                includedItemsAmount = 576,
                taxAmount = 129
            )
        )

        assertThat(actualReport.finalizedPaidTaxes).containsOnly(
            FinalizedGeneralTaxSummaryItem(
                tax = preconditions.generalTax.id!!,
                includedItemsNumber = 1,
                includedItemsAmount = 20,
                taxAmount = 2
            ),

            FinalizedGeneralTaxSummaryItem(
                tax = preconditions.collectedTax2.id!!,
                includedItemsNumber = 2,
                includedItemsAmount = 70,
                taxAmount = 10
            )
        )

        assertThat(actualReport.pendingCollectedTaxes).containsOnly(
            PendingGeneralTaxSummaryItem(
                tax = preconditions.paidTax1.id!!,
                includedItemsNumber = 2
            )
        )

        assertThat(actualReport.pendingPaidTaxes).containsOnly(
            PendingGeneralTaxSummaryItem(
                tax = preconditions.collectedTax1.id!!,
                includedItemsNumber = 1
            )
        )
    }

    private val preconditions by lazyPreconditions {
        object {
            val dateFrom: LocalDate = LocalDate.of(3000, 1, 1)
            val dateTo: LocalDate = LocalDate.of(3010, 1, 1)

            private val bender = bender()

            val planetExpress = workspace(
                owner = bender
            )

            private val leagueOfRobots = workspace(
                name = "League of Robots",
                owner = bender
            )

            private val deliveryCategory = category(
                workspace = planetExpress
            )

            private val secretCategory = category(
                workspace = leagueOfRobots,
                name = "Secret category"
            )

            private val secretTax = generalTax(
                workspace = leagueOfRobots
            )

            val collectedTax1 = generalTax(
                workspace = planetExpress
            )

            val collectedTax2 = generalTax(
                workspace = planetExpress
            )

            val paidTax1 = generalTax(
                workspace = planetExpress
            )

            val generalTax = generalTax(
                workspace = planetExpress
            )

            init {
                expense(
                    category = secretCategory,
                    workspace = leagueOfRobots,
                    datePaid = dateFrom.plusDays(1),
                    originalAmount = 30000,
                    convertedAmounts = amountsInDefaultCurrency(30000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(30000),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    generalTax = secretTax,
                    generalTaxAmount = 4555,
                    status = ExpenseStatus.FINALIZED
                )

                generalTax(
                    workspace = planetExpress
                )

                expense(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = null,
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    datePaid = dateFrom.plusDays(1),
                    status = ExpenseStatus.FINALIZED
                )

                expense(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = generalTax,
                    currency = "ZZG",
                    originalAmount = 30000,
                    convertedAmounts = amountsInDefaultCurrency(100000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(20),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    generalTaxAmount = 2,
                    datePaid = dateFrom.plusDays(1),
                    status = ExpenseStatus.FINALIZED
                )

                expense(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = collectedTax2,
                    originalAmount = 30,
                    incomeTaxableAmounts = amountsInDefaultCurrency(30),
                    convertedAmounts = amountsInDefaultCurrency(30),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    generalTaxAmount = 4,
                    datePaid = dateFrom,
                    status = ExpenseStatus.FINALIZED
                )

                expense(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = collectedTax2,
                    originalAmount = 40,
                    convertedAmounts = amountsInDefaultCurrency(40),
                    incomeTaxableAmounts = amountsInDefaultCurrency(40),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    generalTaxAmount = 6,
                    datePaid = dateTo,
                    status = ExpenseStatus.FINALIZED
                )

                expense(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = null,
                    currency = "ZZG",
                    originalAmount = 30000,
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    datePaid = dateFrom.plusDays(1),
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
                )

                expense(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = collectedTax1,
                    currency = "ZZG",
                    originalAmount = 30000,
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    datePaid = dateFrom.plusDays(1),
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
                )

                expense(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = collectedTax1,
                    originalAmount = 100,
                    convertedAmounts = amountsInDefaultCurrency(100),
                    incomeTaxableAmounts = amountsInDefaultCurrency(100),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    generalTaxAmount = 20,
                    datePaid = dateFrom.minusDays(1),
                    status = ExpenseStatus.FINALIZED
                )

                expense(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = collectedTax1,
                    originalAmount = 100,
                    incomeTaxableAmounts = amountsInDefaultCurrency(100),
                    convertedAmounts = amountsInDefaultCurrency(100),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    generalTaxAmount = 30,
                    datePaid = dateTo.plusDays(1),
                    status = ExpenseStatus.FINALIZED
                )

                income(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = null,
                    currency = planetExpress.defaultCurrency,
                    originalAmount = 100000,
                    convertedAmounts = amountsInDefaultCurrency(100000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(100000),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    dateReceived = dateFrom.plusDays(1)
                )

                income(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = generalTax,
                    currency = planetExpress.defaultCurrency,
                    originalAmount = 500,
                    convertedAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 500,
                        adjustedAmountInDefaultCurrency = 400
                    ),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 500,
                        adjustedAmountInDefaultCurrency = 400
                    ),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    generalTaxAmount = 76,
                    dateReceived = dateFrom.plusDays(1)
                )

                income(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = paidTax1,
                    currency = "ZZH",
                    originalAmount = 100,
                    convertedAmounts = amountsInDefaultCurrency(100000),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 10000,
                        adjustedAmountInDefaultCurrency = 320
                    ),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    generalTaxAmount = 31,
                    dateReceived = dateFrom.plusDays(1)
                )

                income(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = paidTax1,
                    currency = planetExpress.defaultCurrency,
                    originalAmount = 256,
                    convertedAmounts = amountsInDefaultCurrency(256),
                    incomeTaxableAmounts = amountsInDefaultCurrency(256),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    generalTaxAmount = 98,
                    dateReceived = dateFrom.plusDays(1)
                )

                income(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = null,
                    currency = "ZZH",
                    originalAmount = 500,
                    convertedAmounts = amountsInDefaultCurrency(256),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    dateReceived = dateFrom.plusDays(1)
                )

                income(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = paidTax1,
                    currency = "ZZH",
                    originalAmount = 322,
                    convertedAmounts = amountsInDefaultCurrency(256),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    dateReceived = dateFrom.plusDays(1)
                )

                income(
                    category = deliveryCategory,
                    workspace = planetExpress,
                    generalTax = paidTax1,
                    currency = "ZZH",
                    originalAmount = 754,
                    convertedAmounts = amountsInDefaultCurrency(256),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    dateReceived = dateFrom.plusDays(1)
                )
            }
        }
    }
}
