package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.repos.InvoiceRepository
import org.springframework.stereotype.Service

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val incomeService: IncomeService,
    private val timeService: TimeService,
    private val customerService: CustomerService,
    private val generalTaxService: GeneralTaxService,
    private val workspaceService: WorkspaceService
) {

    /**
     * If tax is provided, it is always calculated on top of reported amount
     */
    suspend fun saveInvoice(invoice: Invoice, workspaceId: Long): Invoice {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)
        // todo #222: should provide validate/getValid method for cleaner code (and better performance)
        generalTaxService.getValidGeneralTax(invoice.generalTaxId, workspace)
        customerService.getValidCustomer(invoice.customerId, workspace)
        // todo #222: validate documents
        return withDbContext {
            if (invoice.datePaid != null && invoice.incomeId == null) {
                val income = incomeService.saveIncome(
                    Income(
                        workspaceId = workspace.id!!,
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
                        generalTaxId = invoice.generalTaxId,
                        status = IncomeStatus.PENDING_CONVERSION
                    )
                )
                invoice.incomeId = income.id
            }

            invoiceRepository.save(invoice)
        }
    }

    suspend fun getInvoiceByIdAndWorkspace(id: Long, workspace: Workspace): Invoice? =
        withDbContext {
            invoiceRepository.findByIdAndWorkspace(id, workspace.id!!)
        }

    suspend fun findByIncome(income: Income): Invoice? {
        return withDbContext {
            invoiceRepository.findByIncome(income.id!!)
        }
    }
}
