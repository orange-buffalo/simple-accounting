package io.orangebuffalo.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.repos.InvoiceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val incomeService: IncomeService,
    private val timeService: TimeService,
    private val entityManager: EntityManager
) {

    /**
     * If tax is provided, it is always calculated on top of reported amount
     */
    suspend fun saveInvoice(invoice: Invoice): Invoice {
        return withDbContext {
            if (invoice.datePaid != null && invoice.income == null) {
                val income = incomeService.saveIncome(
                    Income(
                        workspaceId = invoice.customer.workspace.id!!,
                        // todo #14: this is just a convenience method not related to business logic
                        // creation of income can safely be moved to UI side, including i18n
                        title = "Payment for ${invoice.title}",
                        timeRecorded = timeService.currentTime(),
                        dateReceived = invoice.datePaid!!,
                        currency = invoice.currency,
                        originalAmount = invoice.amount,
                        convertedAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = null,
                            adjustedAmountInDefaultCurrency = null
                        ),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        incomeTaxableAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = null,
                            adjustedAmountInDefaultCurrency = null
                        ),
                        categoryId = null,
                        generalTaxId = invoice.generalTax?.id,
                        status = IncomeStatus.PENDING_CONVERSION
                    )
                )
                invoice.income = entityManager.find(LegacyIncome::class.java, income.id)
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
            invoiceRepository.findByIncomeId(income.id!!)
        }
    }
}
