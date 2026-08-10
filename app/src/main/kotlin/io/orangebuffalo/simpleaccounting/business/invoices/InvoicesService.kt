package io.orangebuffalo.simpleaccounting.business.invoices

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.customers.CustomersService
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.TimeService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class InvoicesService(
    private val invoicesRepository: InvoicesRepository,
    private val customersService: CustomersService,
    private val generalTaxesService: GeneralTaxesService,
    private val workspacesService: WorkspacesService,
    private val documentsService: DocumentsService,
    private val timeService: TimeService,
    @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    private val taskExecutor: AsyncTaskExecutor
) {

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        // todo #246: investigate and execute in the same thread if possible
        taskExecutor.submit {
            moveInvoicesToOverdue()
        }
    }

    @Scheduled(cron = "0 1 0 * * *")
    fun moveInvoicesToOverdue() {
        logger.info { "Started moving invoices to overdue" }
        val overdueInvoices = invoicesRepository.findAllOverdue()
        invoicesRepository.saveAll(overdueInvoices.map { it.copy(status = InvoiceStatus.OVERDUE) })
        logger.info { "All eligible invoices moved to overdue state" }
    }

    /**
     * If tax is provided, it is always calculated on top of reported amount
     */
    fun saveInvoice(invoice: Invoice, workspaceId: String): Invoice {
        validateInvoice(invoice, workspaceId)
        return invoicesRepository.save(updateInvoiceStatus(invoice))
    }

    fun cancelInvoice(invoiceId: String, workspaceId: String): Invoice {
        val invoice = invoicesRepository.findById(invoiceId)
            .orElseThrow { throw EntityNotFoundException("Invoice $invoiceId is not found") }
        val customer = customersService.findById(invoice.customerId)
            ?: throw EntityNotFoundException("Customer ${invoice.customerId} is not found")
        workspacesService.validateWorkspaceAccess(
            customer.workspaceId,
            WorkspaceAccessMode.READ_WRITE
        )

        if (workspaceId != customer.workspaceId) {
            throw EntityNotFoundException("Invoice $invoiceId is not found")
        }

        return invoicesRepository.save(
                invoice.copy(
                    status = InvoiceStatus.CANCELLED,
                    timeCancelled = timeService.currentTime(),
                )
            )
    }

    private fun updateInvoiceStatus(invoice: Invoice): Invoice = if (invoice.status == InvoiceStatus.CANCELLED) {
        invoice
    } else if (invoice.datePaid != null) {
        invoice.copy(status = InvoiceStatus.PAID)
    } else if (invoice.dateSent != null && isOverdue(invoice)) {
        invoice.copy(status = InvoiceStatus.OVERDUE)
    } else if (invoice.dateSent != null) {
        invoice.copy(status = InvoiceStatus.SENT)
    } else {
        invoice.copy(status = InvoiceStatus.DRAFT)
    }

    private fun isOverdue(invoice: Invoice) = invoice.dueDate.isBefore(timeService.currentDate())

    private fun validateInvoice(
        invoice: Invoice,
        workspaceId: String
    ) {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)
        validateGeneralTax(invoice, workspaceId)
        customersService.validateCustomer(invoice.customerId, workspaceId)
        validateAttachments(invoice, workspaceId)
    }

    private fun validateGeneralTax(
        invoice: Invoice,
        workspaceId: String
    ) {
        if (invoice.generalTaxId != null) {
            generalTaxesService.validateGeneralTax(invoice.generalTaxId!!, workspaceId)
        }
    }

    private fun validateAttachments(invoice: Invoice, workspaceId: String) {
        if (invoice.attachments.isNotEmpty()) {
            val attachmentsIds = invoice.attachments.map { it.documentId }
            documentsService.validateDocuments(workspaceId, attachmentsIds)
        }
    }

    fun getInvoiceByIdAndWorkspaceId(id: String, workspaceId: String): Invoice? =
        invoicesRepository.findByIdAndWorkspaceId(id, workspaceId)
}
