package io.orangebuffalo.simpleaccounting.domain.invoices

import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentDate
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

@SimpleAccountingIntegrationTest
class InvoicesServiceIT(
    @Autowired private val invoicesService: InvoicesService,
    @Autowired private val invoiceRepository: InvoiceRepository
) {

    @field:MockBean
    private lateinit var timeService: TimeService

    @Test
    fun `should move proper invoices into Overdue`(testData: OverdueJobTestData) {
        mockCurrentDate(timeService)

        invoicesService.moveInvoicesToOverdue()

        testData.overdueInvoices.forEach {
            val maybeInvoice = invoiceRepository.findById(it.id!!)
            assertThat(maybeInvoice).hasValueSatisfying { invoice ->
                assertThat(invoice.status).isEqualTo(InvoiceStatus.OVERDUE)
            }
        }

        testData.unchangedInvoices.forEach {
            val maybeInvoice = invoiceRepository.findById(it.id!!)
            assertThat(maybeInvoice).hasValueSatisfying { invoice ->
                assertThat(invoice.status).isNotEqualTo(InvoiceStatus.OVERDUE)
            }
        }
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class OverdueJobTestData : TestDataDeprecated {
    val firstOwner = Prototypes.platformUser()
    val secondOwner = Prototypes.platformUser()
    val firstWorkspace = Prototypes.workspace(owner = firstOwner)
    val secondWorkspace = Prototypes.workspace(owner = secondOwner)
    val firstCustomer = Prototypes.customer(workspace = firstWorkspace)
    val secondCustomer = Prototypes.customer(workspace = secondWorkspace)
    val overdueInvoices = listOf(
        Prototypes.invoice(
            customer = firstCustomer,
            status = InvoiceStatus.SENT,
            dueDate = MOCK_DATE.minusDays(1)
        ),
        Prototypes.invoice(
            customer = secondCustomer,
            status = InvoiceStatus.SENT,
            dueDate = MOCK_DATE.minusDays(1)
        )
    )
    val unchangedInvoices = listOf(
        Prototypes.invoice(
            customer = secondCustomer,
            status = InvoiceStatus.SENT,
            dueDate = MOCK_DATE
        ),
        Prototypes.invoice(
            customer = secondCustomer,
            status = InvoiceStatus.SENT,
            dueDate = MOCK_DATE.plusDays(1)
        ),
        Prototypes.invoice(
            customer = firstCustomer,
            status = InvoiceStatus.DRAFT,
            dueDate = MOCK_DATE.minusDays(1)
        ),
        Prototypes.invoice(
            customer = secondCustomer,
            status = InvoiceStatus.DRAFT,
            dueDate = MOCK_DATE
        ),
        Prototypes.invoice(
            customer = secondCustomer,
            status = InvoiceStatus.PAID,
            dueDate = MOCK_DATE.minusDays(1)
        ),
        Prototypes.invoice(
            customer = firstCustomer,
            status = InvoiceStatus.PAID,
            dueDate = MOCK_DATE
        )
    )

    override fun generateData() = listOf(
        firstOwner, firstWorkspace, firstCustomer,
        secondOwner, secondWorkspace, secondCustomer,
        *overdueInvoices.toTypedArray(),
        *unchangedInvoices.toTypedArray()
    )
}
