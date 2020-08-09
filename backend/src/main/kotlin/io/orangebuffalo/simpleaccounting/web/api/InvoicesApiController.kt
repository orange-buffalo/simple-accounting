package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.InvoiceService
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.simpleaccounting.services.persistence.entities.InvoiceAttachment
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiPredicateOperator
import org.hibernate.validator.constraints.Length
import org.jooq.impl.DSL.or
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/invoices")
class InvoicesApiController(
    private val invoiceService: InvoiceService,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @PostMapping
    suspend fun createInvoice(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditInvoiceDto
    ): InvoiceDto = invoiceService
        .saveInvoice(
            Invoice(
                title = request.title,
                timeRecorded = timeService.currentTime(),
                customerId = request.customer,
                currency = request.currency,
                notes = request.notes,
                attachments = getInvoiceAttachments(request.attachments),
                dateIssued = request.dateIssued,
                dateSent = request.dateSent,
                datePaid = request.datePaid,
                dateCancelled = request.dateCancelled,
                dueDate = request.dueDate,
                amount = request.amount,
                generalTaxId = request.generalTax
            ),
            workspaceId
        )
        .mapToInvoiceDto(timeService)

    private fun getInvoiceAttachments(attachments: List<Long>?): Set<InvoiceAttachment> =
        attachments?.asSequence()?.map(::InvoiceAttachment)?.toSet() ?: emptySet()

    @GetMapping
    suspend fun getInvoices(@PathVariable workspaceId: Long) = filteringApiExecutor.executeFiltering(workspaceId)

    @GetMapping("{invoiceId}")
    suspend fun getInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long
    ): InvoiceDto {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        // todo #71: when optimistic locking is addressed, move access control into the business service
        val invoice = invoiceService.getInvoiceByIdAndWorkspaceId(invoiceId, workspaceId)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")
        return invoice.mapToInvoiceDto(timeService)
    }

    @PutMapping("{invoiceId}")
    suspend fun updateInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long,
        @RequestBody @Valid request: EditInvoiceDto
    ): InvoiceDto {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val invoice = invoiceService.getInvoiceByIdAndWorkspaceId(invoiceId, workspaceId)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")

        return invoice
            .apply {
                title = request.title
                customerId = request.customer
                currency = request.currency
                notes = request.notes
                attachments = getInvoiceAttachments(request.attachments)
                dateIssued = request.dateIssued
                dateSent = request.dateSent
                datePaid = request.datePaid
                dateCancelled = request.dateCancelled
                dueDate = request.dueDate
                amount = request.amount
                generalTaxId = request.generalTax
            }
            .let { invoiceService.saveInvoice(it, workspaceId) }
            .mapToInvoiceDto(timeService)
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder.executor<Invoice, InvoiceDto> {
        query(Tables.INVOICE) {
            val customer = Tables.CUSTOMER.`as`("filterCustomer")

            configure {
                query.join(customer).on(customer.id.eq(root.customerId))
            }

            filterByField("freeSearchText", String::class) {
                onPredicate(FilteringApiPredicateOperator.EQ) { filter ->
                    or(
                        root.notes.containsIgnoreCase(filter),
                        root.title.containsIgnoreCase(filter),
                        customer.name.containsIgnoreCase(filter)
                    )
                }
            }
            filterByField("status", InvoiceStatus::class) {
                // todo 103: migrate to denormalized presentation and cover with filtering tests
                onPredicate(FilteringApiPredicateOperator.IN) { statuses ->
                    or(statuses.map { status ->
                        when (status) {
                            InvoiceStatus.DRAFT -> root.datePaid.isNull
                                .and(root.dateSent.isNull)
                                .and(root.dateCancelled.isNull)
                            InvoiceStatus.CANCELLED -> root.dateCancelled.isNotNull
                            InvoiceStatus.PAID -> root.datePaid.isNotNull
                                .and(root.dateCancelled.isNull)
                            InvoiceStatus.SENT -> root.dateSent.isNotNull
                                .and(root.datePaid.isNull)
                                .and(root.dateCancelled.isNull)
                                .and(root.dueDate.greaterOrEqual(timeService.currentDate()))
                            InvoiceStatus.OVERDUE -> root.datePaid.isNull
                                .and(root.dateCancelled.isNull)
                                .and(root.dueDate.lt(timeService.currentDate()))
                        }
                    })
                }
            }
            addDefaultSorting { root.dateIssued.desc() }
            addDefaultSorting { root.timeRecorded.asc() }
            workspaceFilter { workspaceId -> customer.workspaceId.eq(workspaceId) }
        }
        mapper { mapToInvoiceDto(timeService) }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvoiceDto(
    val title: String,
    val customer: Long,
    val timeRecorded: Instant,
    val dateIssued: LocalDate,
    val dateSent: LocalDate?,
    val datePaid: LocalDate?,
    val dateCancelled: LocalDate?,
    val dueDate: LocalDate,
    val currency: String,
    val amount: Long,
    val attachments: List<Long>,
    val notes: String?,
    val id: Long,
    val version: Int,
    val status: InvoiceStatus,
    val generalTax: Long?
)

enum class InvoiceStatus {
    DRAFT,
    SENT,
    OVERDUE,
    PAID,
    CANCELLED
}

data class EditInvoiceDto(
    @field:NotBlank @field:Length(max = 255) val title: String,
    val customer: Long,
    val dateIssued: LocalDate,
    val dateSent: LocalDate?,
    val datePaid: LocalDate?,
    val dateCancelled: LocalDate?,
    val dueDate: LocalDate,
    @field:NotBlank @field:Length(max = 3) val currency: String,
    val amount: Long,
    val attachments: List<Long>?,
    @field:Size(max = 1024) val notes: String?,
    val generalTax: Long?
)

private fun Invoice.mapToInvoiceDto(timeService: TimeService) = InvoiceDto(
    title = title,
    customer = customerId,
    timeRecorded = timeRecorded,
    dateIssued = dateIssued,
    dateCancelled = dateCancelled,
    datePaid = datePaid,
    dateSent = dateSent,
    dueDate = dueDate,
    currency = currency,
    amount = amount,
    attachments = attachments.map { it.documentId },
    notes = notes,
    id = id!!,
    version = version!!,
    status = getInvoiceStatus(this, timeService),
    generalTax = generalTaxId
)

private fun getInvoiceStatus(invoice: Invoice, timeService: TimeService): InvoiceStatus = when {
    invoice.dateCancelled != null -> InvoiceStatus.CANCELLED
    invoice.datePaid != null -> InvoiceStatus.PAID
    invoice.dueDate.isBefore(timeService.currentDate()) -> InvoiceStatus.OVERDUE
    invoice.dateSent != null -> InvoiceStatus.SENT
    else -> InvoiceStatus.DRAFT
}
