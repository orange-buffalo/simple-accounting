package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.executeInParallel
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.simpleaccounting.services.persistence.repos.InvoiceRepository
import org.springframework.stereotype.Service

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
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
        return withDbContext { invoiceRepository.save(invoice) }
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
}
