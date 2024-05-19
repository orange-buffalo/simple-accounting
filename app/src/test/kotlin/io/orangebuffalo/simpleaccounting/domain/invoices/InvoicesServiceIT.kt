package io.orangebuffalo.simpleaccounting.domain.invoices

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentDate
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

@SimpleAccountingIntegrationTest
class InvoicesServiceIT(
    @Autowired private val invoicesService: InvoicesService,
    @Autowired private val invoiceRepository: InvoiceRepository,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @field:MockBean
    private lateinit var timeService: TimeService

    @Test
    fun `should move proper invoices into Overdue`() {
        val testData = setupPreconditions()
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

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val firstOwner = platformUser()
        val secondOwner = platformUser()
        val firstWorkspace = workspace(owner = firstOwner)
        val secondWorkspace = workspace(owner = secondOwner)
        val firstCustomer = customer(workspace = firstWorkspace)
        val secondCustomer = customer(workspace = secondWorkspace)
        val overdueInvoices = listOf(
            invoice(
                customer = firstCustomer,
                status = InvoiceStatus.SENT,
                dueDate = MOCK_DATE.minusDays(1)
            ),
            invoice(
                customer = secondCustomer,
                status = InvoiceStatus.SENT,
                dueDate = MOCK_DATE.minusDays(1)
            )
        )
        val unchangedInvoices = listOf(
            invoice(
                customer = secondCustomer,
                status = InvoiceStatus.SENT,
                dueDate = MOCK_DATE
            ),
            invoice(
                customer = secondCustomer,
                status = InvoiceStatus.SENT,
                dueDate = MOCK_DATE.plusDays(1)
            ),
            invoice(
                customer = firstCustomer,
                status = InvoiceStatus.DRAFT,
                dueDate = MOCK_DATE.minusDays(1)
            ),
            invoice(
                customer = secondCustomer,
                status = InvoiceStatus.DRAFT,
                dueDate = MOCK_DATE
            ),
            invoice(
                customer = secondCustomer,
                status = InvoiceStatus.PAID,
                dueDate = MOCK_DATE.minusDays(1)
            ),
            invoice(
                customer = firstCustomer,
                status = InvoiceStatus.PAID,
                dueDate = MOCK_DATE
            )
        )
    }
}
