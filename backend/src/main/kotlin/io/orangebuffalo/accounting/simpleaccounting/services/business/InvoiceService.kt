package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
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
    private val incomeRepository: IncomeRepository
) {

    suspend fun saveInvoice(invoice: Invoice): Invoice {
        //todo create income if paid (blocked by mandatory category in income)
        return withDbContext {
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

    suspend fun getInvoiceByIdAndWorkspace(id: Long, workspace: Workspace): Invoice? = withDbContext {
        invoiceRepository.findByIdAndCustomerWorkspace(id, workspace)
    }
}