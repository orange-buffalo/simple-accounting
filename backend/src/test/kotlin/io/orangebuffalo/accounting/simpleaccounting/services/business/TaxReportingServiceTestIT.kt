package io.orangebuffalo.accounting.simpleaccounting.services.business

import assertk.assertThat
import assertk.assertions.containsOnly
import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.FinalizedTaxSummaryItem
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.PendingTaxSummaryItem
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
internal class TaxReportingServiceTestIT(
    @Autowired private val taxReportingService: TaxReportingService
) {

    @Test
    fun `should calculate tax report`(testData: TaxReportTestData) {
        val actualReport = runBlocking {
            taxReportingService.getTaxReport(testData.dateFrom, testData.dateTo, testData.planetExpress)
        }

        assertThat(actualReport.finalizedCollectedTaxes).containsOnly(
            FinalizedTaxSummaryItem(
                tax = testData.generalTax.id!!,
                includedItemsNumber = 1,
                includedItemsAmount = 400,
                taxAmount = 76
            ),
            FinalizedTaxSummaryItem(
                tax = testData.paidTax1.id!!,
                includedItemsNumber = 2,
                includedItemsAmount = 576,
                taxAmount = 129
            )
        )

        assertThat(actualReport.finalizedPaidTaxes).containsOnly(
            FinalizedTaxSummaryItem(
                tax = testData.generalTax.id!!,
                includedItemsNumber = 1,
                includedItemsAmount = 20,
                taxAmount = 2
            ),

            FinalizedTaxSummaryItem(
                tax = testData.collectedTax2.id!!,
                includedItemsNumber = 2,
                includedItemsAmount = 70,
                taxAmount = 10
            )
        )

        assertThat(actualReport.pendingCollectedTaxes).containsOnly(
            PendingTaxSummaryItem(
                tax = testData.paidTax1.id!!,
                includedItemsNumber = 2
            )
        )

        assertThat(actualReport.pendingPaidTaxes).containsOnly(
            PendingTaxSummaryItem(
                tax = testData.collectedTax1.id!!,
                includedItemsNumber = 1
            )
        )
    }
}

class TaxReportTestData : TestData {

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

    private val secretTax = Prototypes.tax(
        workspace = leagueOfRobots
    )

    val collectedTax1 = Prototypes.tax(
        workspace = planetExpress
    )

    val collectedTax2 = Prototypes.tax(
        workspace = planetExpress
    )

    val paidTax1 = Prototypes.tax(
        workspace = planetExpress
    )

    val generalTax = Prototypes.tax(
        workspace = planetExpress
    )

    override fun generateData() = listOf(
        bender, planetExpress, deliveryCategory,
        leagueOfRobots, secretCategory, secretTax, Prototypes.expense(
            category = secretCategory,
            workspace = leagueOfRobots,
            datePaid = dateFrom.plusDays(1),
            reportedAmountInDefaultCurrency = 30000,
            tax = secretTax,
            taxAmount = 4555
        ),
        collectedTax1, collectedTax2, paidTax1, generalTax,

        Prototypes.tax(
            workspace = planetExpress
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = null,
            reportedAmountInDefaultCurrency = 10000,
            datePaid = dateFrom.plusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = generalTax,
            reportedAmountInDefaultCurrency = 20,
            taxAmount = 2,
            datePaid = dateFrom.plusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = collectedTax2,
            reportedAmountInDefaultCurrency = 30,
            taxAmount = 4,
            datePaid = dateFrom
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = collectedTax2,
            reportedAmountInDefaultCurrency = 40,
            taxAmount = 6,
            datePaid = dateTo
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = null,
            reportedAmountInDefaultCurrency = 0,
            datePaid = dateFrom.plusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = collectedTax1,
            reportedAmountInDefaultCurrency = 0,
            datePaid = dateFrom.plusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = collectedTax1,
            reportedAmountInDefaultCurrency = 100,
            taxAmount = 20,
            datePaid = dateFrom.minusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = collectedTax1,
            reportedAmountInDefaultCurrency = 100,
            taxAmount = 30,
            datePaid = dateTo.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = null,
            reportedAmountInDefaultCurrency = 100000,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = generalTax,
            reportedAmountInDefaultCurrency = 400,
            taxAmount = 76,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = paidTax1,
            reportedAmountInDefaultCurrency = 320,
            taxAmount = 31,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = paidTax1,
            reportedAmountInDefaultCurrency = 256,
            taxAmount = 98,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = null,
            reportedAmountInDefaultCurrency = 0,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = paidTax1,
            reportedAmountInDefaultCurrency = 0,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            tax = paidTax1,
            reportedAmountInDefaultCurrency = 0,
            dateReceived = dateFrom.plusDays(1)
        )
    )
}