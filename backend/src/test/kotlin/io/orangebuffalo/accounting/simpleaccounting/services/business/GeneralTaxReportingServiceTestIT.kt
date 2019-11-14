package io.orangebuffalo.accounting.simpleaccounting.services.business

import assertk.assertThat
import assertk.assertions.containsOnly
import io.orangebuffalo.accounting.simpleaccounting.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.AmountsInDefaultCurrency
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.ExpenseStatus
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.IncomeStatus
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.FinalizedGeneralTaxSummaryItem
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.PendingGeneralTaxSummaryItem
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
internal class GeneralTaxReportingServiceTestIT(
    @Autowired private val taxReportingService: GeneralTaxReportingService
) {

    @Test
    fun `should calculate general tax report`(testData: GeneralTaxReportTestData) {
        val actualReport = runBlocking {
            taxReportingService.getGeneralTaxReport(testData.dateFrom, testData.dateTo, testData.planetExpress)
        }

        assertThat(actualReport.finalizedCollectedTaxes).containsOnly(
            FinalizedGeneralTaxSummaryItem(
                tax = testData.generalTax.id!!,
                includedItemsNumber = 1,
                includedItemsAmount = 400,
                taxAmount = 76
            ),
            FinalizedGeneralTaxSummaryItem(
                tax = testData.paidTax1.id!!,
                includedItemsNumber = 2,
                includedItemsAmount = 576,
                taxAmount = 129
            )
        )

        assertThat(actualReport.finalizedPaidTaxes).containsOnly(
            FinalizedGeneralTaxSummaryItem(
                tax = testData.generalTax.id!!,
                includedItemsNumber = 1,
                includedItemsAmount = 20,
                taxAmount = 2
            ),

            FinalizedGeneralTaxSummaryItem(
                tax = testData.collectedTax2.id!!,
                includedItemsNumber = 2,
                includedItemsAmount = 70,
                taxAmount = 10
            )
        )

        assertThat(actualReport.pendingCollectedTaxes).containsOnly(
            PendingGeneralTaxSummaryItem(
                tax = testData.paidTax1.id!!,
                includedItemsNumber = 2
            )
        )

        assertThat(actualReport.pendingPaidTaxes).containsOnly(
            PendingGeneralTaxSummaryItem(
                tax = testData.collectedTax1.id!!,
                includedItemsNumber = 1
            )
        )
    }
}

class GeneralTaxReportTestData : TestData {

    val dateFrom: LocalDate = LocalDate.of(3000, 1, 1)
    val dateTo: LocalDate = LocalDate.of(3010, 1, 1)

    private val bender = Prototypes.bender()

    val planetExpress = Prototypes.workspace(
        owner = bender
    )

    private val leagueOfRobots = Prototypes.workspace(
        name = "League of Robots",
        owner = bender
    )

    private val deliveryCategory = Prototypes.category(
        workspace = planetExpress
    )

    private val secretCategory = Prototypes.category(
        workspace = leagueOfRobots,
        name = "Secret category"
    )

    private val secretTax = Prototypes.generalTax(
        workspace = leagueOfRobots
    )

    val collectedTax1 = Prototypes.generalTax(
        workspace = planetExpress
    )

    val collectedTax2 = Prototypes.generalTax(
        workspace = planetExpress
    )

    val paidTax1 = Prototypes.generalTax(
        workspace = planetExpress
    )

    val generalTax = Prototypes.generalTax(
        workspace = planetExpress
    )

    override fun generateData() = listOf(
        bender, planetExpress, deliveryCategory,
        leagueOfRobots, secretCategory, secretTax, Prototypes.expense(
            category = secretCategory,
            workspace = leagueOfRobots,
            datePaid = dateFrom.plusDays(1),
            originalAmount = 30000,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(30000),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(30000),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            generalTax = secretTax,
            generalTaxAmount = 4555,
            status = ExpenseStatus.FINALIZED
        ),
        collectedTax1, collectedTax2, paidTax1, generalTax,

        Prototypes.generalTax(
            workspace = planetExpress
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = null,
            originalAmount = 10000,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(10000),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(10000),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            datePaid = dateFrom.plusDays(1),
            status = ExpenseStatus.FINALIZED
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = generalTax,
            currency = "ZZG",
            originalAmount = 30000,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(100000),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(20),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            generalTaxAmount = 2,
            datePaid = dateFrom.plusDays(1),
            status = ExpenseStatus.FINALIZED
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax2,
            originalAmount = 30,
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(30),
            convertedAmounts = Prototypes.amountsInDefaultCurrency(30),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            generalTaxAmount = 4,
            datePaid = dateFrom,
            status = ExpenseStatus.FINALIZED
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax2,
            originalAmount = 40,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(40),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(40),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            generalTaxAmount = 6,
            datePaid = dateTo,
            status = ExpenseStatus.FINALIZED
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = null,
            currency = "ZZG",
            originalAmount = 30000,
            convertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            datePaid = dateFrom.plusDays(1),
            status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax1,
            currency = "ZZG",
            originalAmount = 30000,
            convertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            datePaid = dateFrom.plusDays(1),
            status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax1,
            originalAmount = 100,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(100),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(100),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            generalTaxAmount = 20,
            datePaid = dateFrom.minusDays(1),
            status = ExpenseStatus.FINALIZED
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax1,
            originalAmount = 100,
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(100),
            convertedAmounts = Prototypes.amountsInDefaultCurrency(100),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            generalTaxAmount = 30,
            datePaid = dateTo.plusDays(1),
            status = ExpenseStatus.FINALIZED
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = null,
            currency = planetExpress.defaultCurrency,
            originalAmount = 100000,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(100000),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(100000),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
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
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = paidTax1,
            currency = "ZZH",
            originalAmount = 100,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(100000),
            incomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 10000,
                adjustedAmountInDefaultCurrency = 320
            ),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            generalTaxAmount = 31,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = paidTax1,
            currency = planetExpress.defaultCurrency,
            originalAmount = 256,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(256),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(256),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            generalTaxAmount = 98,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = null,
            currency = "ZZH",
            originalAmount = 500,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(256),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = paidTax1,
            currency = "ZZH",
            originalAmount = 322,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(256),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = paidTax1,
            currency = "ZZH",
            originalAmount = 754,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(256),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
            dateReceived = dateFrom.plusDays(1)
        )
    )
}
