package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.executeInParallel
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
    private val workspaceService: WorkspaceService,
    private val documentsService: DocumentsService
) {

    /**
     * If tax is provided, it is always calculated on top of reported amount
     */
    suspend fun saveInvoice(invoice: Invoice, workspaceId: Long): Invoice {
        validateInvoice(invoice, workspaceId)
        createIncomeIfNecessary(invoice, workspaceId)
        return withDbContext { invoiceRepository.save(invoice) }
    }

    private suspend fun createIncomeIfNecessary(
        invoice: Invoice,
        workspaceId: Long
    ) {
        if (invoice.datePaid != null && invoice.incomeId == null) {
            val income = incomeService.saveIncome(
                Income(
                    workspaceId = workspaceId,
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
    }

    private suspend fun validateInvoice(
        invoice: Invoice,
        workspaceId: Long
    ) = executeInParallel {
        step { workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE) }
        step { validateGeneralTax(invoice, workspaceId) }
        step { customerService.validateCustomer(invoice.customerId, workspaceId) }
        step { validateAttachments(invoice, workspaceId) }
    }

    private suspend fun validateGeneralTax(
        invoice: Invoice,
        workspaceId: Long
    ) {
        if (invoice.generalTaxId != null) {
            generalTaxService.validateGeneralTax(invoice.generalTaxId!!, workspaceId)
        }
    }

    private suspend fun validateAttachments(invoice: Invoice, workspaceId: Long) {
        if (invoice.attachments.isNotEmpty()) {
            val attachmentsIds = invoice.attachments.map { it.documentId }
            documentsService.validateDocuments(workspaceId, attachmentsIds)
        }
    }

    suspend fun getInvoiceByIdAndWorkspaceId(id: Long, workspaceId: Long): Invoice? = withDbContext {
        invoiceRepository.findByIdAndWorkspaceId(id, workspaceId)
    }

    suspend fun findByIncome(income: Income): Invoice? = withDbContext {
        invoiceRepository.findByIncomeId(income.id!!)
    }
}
