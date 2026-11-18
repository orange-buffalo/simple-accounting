package io.orangebuffalo.simpleaccounting.business.invoices

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.customers.CustomersService
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesService
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import java.time.ZoneId
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class InvoicesService(
    private val invoicesRepository: InvoicesRepository,
    private val customersService: CustomersService,
    private val generalTaxesService: GeneralTaxesService,
    private val documentsService: DocumentsService,
    private val timeService: TimeService,
    private val workspacesService: WorkspacesService,
    private val usersService: PlatformUsersService,
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

    @Scheduled(cron = "0 * * * * *")
    fun moveInvoicesToOverdue() {
        logger.info { "Started moving invoices to overdue" }
        val overdueInvoices = invoicesRepository.findAllOverdue()
        invoicesRepository.saveAll(overdueInvoices.map { it.copy(status = InvoiceStatus.OVERDUE) })
        logger.info { "All eligible invoices moved to overdue state" }
    }

    /**
     * If tax is provided, it is always calculated on top of reported amount
     */
    fun saveInvoice(invoice: Invoice): Invoice {
        validateInvoice(invoice = invoice, workspaceId = invoice.workspaceId)
        return invoicesRepository.save(updateInvoiceStatus(invoice))
    }

    fun cancelInvoice(invoice: Invoice, workspaceId: String): Invoice {
        if (workspaceId != invoice.workspaceId) {
            throw EntityNotFoundException("Invoice ${invoice.id} is not found")
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

    private fun isOverdue(invoice: Invoice): Boolean {
        val workspace = workspacesService.getWorkspace(invoice.workspaceId)
        val owner = usersService.getUserByUserId(workspace.ownerId)
        return invoice.dueDate.isBefore(timeService.currentDate(ZoneId.of(owner.timeZone)))
    }

    private fun validateInvoice(
        invoice: Invoice,
        workspaceId: String
    ) {
        validateGeneralTax(invoice, workspaceId)
        customersService.validateCustomer(customerId = invoice.customerId, workspaceId = workspaceId)
        validateAttachments(invoice, workspaceId)
    }

    private fun validateGeneralTax(
        invoice: Invoice,
        workspaceId: String
    ) {
        if (invoice.generalTaxId != null) {
            generalTaxesService.validateGeneralTax(taxId = invoice.generalTaxId, workspaceId = workspaceId)
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

    fun getInvoiceById(id: String): Invoice? = invoicesRepository.findByIdOrNull(id)
}
