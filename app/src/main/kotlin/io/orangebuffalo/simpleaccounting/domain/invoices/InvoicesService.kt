package io.orangebuffalo.simpleaccounting.domain.invoices

import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.services.business.*
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.executeInParallel
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

private val logger = KotlinLogging.logger {}

@Service
class InvoicesService(
    private val invoiceRepository: InvoiceRepository,
    private val customerService: CustomerService,
    private val generalTaxService: GeneralTaxService,
    private val workspaceService: WorkspaceService,
    private val documentsService: DocumentsService,
    private val timeService: TimeService,
    private val taskExecutor: AsyncTaskExecutor
) {

    @PostConstruct
    fun init() {
        // todo #246: investigate and execute in the same thread if possible
        taskExecutor.submit {
            moveInvoicesToOverdue()
        }
    }

    @Scheduled(cron = "0 1 0 * * *")
    fun moveInvoicesToOverdue() {
        logger.info { "Started moving invoices to overdue" }
        runBlocking {
            val overdueInvoices = withDbContext { invoiceRepository.findAllOverdue() }
            overdueInvoices.forEach { invoice ->
                invoice.status = InvoiceStatus.OVERDUE
            }
            withDbContext { invoiceRepository.saveAll(overdueInvoices) }
        }
        logger.info { "All eligible invoices moved to overdue state" }
    }

    /**
     * If tax is provided, it is always calculated on top of reported amount
     */
    suspend fun saveInvoice(invoice: Invoice, workspaceId: Long): Invoice {
        validateInvoice(invoice, workspaceId)
        updateInvoiceStatus(invoice)
        return withDbContext { invoiceRepository.save(invoice) }
    }

    suspend fun cancelInvoice(invoiceId: Long, workspaceId: Long): Invoice {
        val invoice = withDbContext {
            invoiceRepository.findById(invoiceId)
                .orElseThrow { throw EntityNotFoundException("Invoice $invoiceId is not found") }
        }
        val customer = customerService.findById(invoice.customerId)
            ?: throw  EntityNotFoundException("Customer ${invoice.customerId} is not found")
        workspaceService.validateWorkspaceAccess(
            customer.workspaceId,
            WorkspaceAccessMode.READ_WRITE
        )

        if (workspaceId != customer.workspaceId) {
            throw EntityNotFoundException("Invoice $invoiceId is not found")
        }

        invoice.status = InvoiceStatus.CANCELLED
        invoice.timeCancelled = timeService.currentTime()
        return withDbContext { invoiceRepository.save(invoice) }
    }

    private fun updateInvoiceStatus(invoice: Invoice) {
        if (invoice.status != InvoiceStatus.CANCELLED) {
            if (invoice.datePaid != null) {
                invoice.status = InvoiceStatus.PAID
            } else if (invoice.dateSent != null && isOverdue(invoice)) {
                invoice.status = InvoiceStatus.OVERDUE
            } else if (invoice.dateSent != null) {
                invoice.status = InvoiceStatus.SENT
            } else {
                invoice.status = InvoiceStatus.DRAFT
            }
        }
    }

    private fun isOverdue(invoice: Invoice) = invoice.dueDate.isBefore(timeService.currentDate())

    private suspend fun validateInvoice(
        invoice: Invoice,
        workspaceId: Long
    ) = executeInParallel {
        step {
            workspaceService.validateWorkspaceAccess(
                workspaceId,
                WorkspaceAccessMode.READ_WRITE
            )
        }
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
