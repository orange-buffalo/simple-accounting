package io.orangebuffalo.accounting.simpleaccounting.services.business

import assertk.assertThat
import assertk.assertions.containsOnly
import io.orangebuffalo.accounting.simpleaccounting.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.FinalizedGeneralTaxSummaryItem
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.PendingGeneralTaxSummaryItem
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
            reportedAmountInDefaultCurrency = 30000,
            generalTax = secretTax,
            generalTaxAmount = 4555
        ),
        collectedTax1, collectedTax2, paidTax1, generalTax,

        Prototypes.generalTax(
            workspace = planetExpress
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = null,
            reportedAmountInDefaultCurrency = 10000,
            datePaid = dateFrom.plusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = generalTax,
            reportedAmountInDefaultCurrency = 20,
            generalTaxAmount = 2,
            datePaid = dateFrom.plusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax2,
            reportedAmountInDefaultCurrency = 30,
            generalTaxAmount = 4,
            datePaid = dateFrom
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax2,
            reportedAmountInDefaultCurrency = 40,
            generalTaxAmount = 6,
            datePaid = dateTo
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = null,
            reportedAmountInDefaultCurrency = 0,
            datePaid = dateFrom.plusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax1,
            reportedAmountInDefaultCurrency = 0,
            datePaid = dateFrom.plusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax1,
            reportedAmountInDefaultCurrency = 100,
            generalTaxAmount = 20,
            datePaid = dateFrom.minusDays(1)
        ),

        Prototypes.expense(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = collectedTax1,
            reportedAmountInDefaultCurrency = 100,
            generalTaxAmount = 30,
            datePaid = dateTo.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = null,
            reportedAmountInDefaultCurrency = 100000,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = generalTax,
            reportedAmountInDefaultCurrency = 400,
            generalTaxAmount = 76,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = paidTax1,
            reportedAmountInDefaultCurrency = 320,
            generalTaxAmount = 31,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = paidTax1,
            reportedAmountInDefaultCurrency = 256,
            generalTaxAmount = 98,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = null,
            reportedAmountInDefaultCurrency = 0,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = paidTax1,
            reportedAmountInDefaultCurrency = 0,
            dateReceived = dateFrom.plusDays(1)
        ),

        Prototypes.income(
            category = deliveryCategory,
            workspace = planetExpress,
            generalTax = paidTax1,
            reportedAmountInDefaultCurrency = 0,
            dateReceived = dateFrom.plusDays(1)
        )
    )
}
