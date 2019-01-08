package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QInvoice
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.InvoiceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val incomeRepository: IncomeRepository,
    private val timeService: TimeService
) {

    suspend fun saveInvoice(invoice: Invoice): Invoice {
        return withDbContext {
            if (invoice.datePaid != null && invoice.income == null) {
                invoice.income = incomeRepository.save(
                    Income(
                        workspace = invoice.customer.workspace,
                        // todo i18n
                        title = "Payment for ${invoice.title}",
                        timeRecorded = timeService.currentTime(),
                        dateReceived = invoice.datePaid!!,
                        currency = invoice.currency,
                        originalAmount = invoice.amount,
                        reportedAmountInDefaultCurrency = 0,
                        amountInDefaultCurrency = 0,
                        category = null
                    )
                )
            }

            invoiceRepository.save(invoice)
        }
    }

    suspend fun getInvoices(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Invoice> = withDbContext {
        invoiceRepository.findAll(QInvoice.invoice.customer.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getInvoiceByIdAndWorkspace(id: Long, workspace: Workspace): Invoice? =
        withDbContext {
            invoiceRepository.findByIdAndCustomerWorkspace(id, workspace)
        }

    suspend fun findByIncome(income: Income): Invoice? {
        return withDbContext {
            invoiceRepository.findByIncome(income)
        }
    }
}