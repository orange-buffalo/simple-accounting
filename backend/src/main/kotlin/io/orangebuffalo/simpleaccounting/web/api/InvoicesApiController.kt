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
        .let { mapInvoiceDto(it, timeService) }

    private fun getInvoiceAttachments(attachments: List<Long>?): Set<InvoiceAttachment> =
        attachments?.asSequence()?.map { documentId -> InvoiceAttachment(documentId) }?.toSet() ?: emptySet()

    @GetMapping
    suspend fun getInvoices(@PathVariable workspaceId: Long) = filteringApiExecutor.executeFiltering(workspaceId)

    @GetMapping("{invoiceId}")
    suspend fun getInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long
    ): InvoiceDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        // todo #222: move access control into the business service
        val invoice = invoiceService.getInvoiceByIdAndWorkspace(invoiceId, workspace)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")
        return mapInvoiceDto(invoice, timeService)
    }

    @PutMapping("{invoiceId}")
    suspend fun updateInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long,
        @RequestBody @Valid request: EditInvoiceDto
    ): InvoiceDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        // todo #222: workspace id only
        val invoice = invoiceService.getInvoiceByIdAndWorkspace(invoiceId, workspace)
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
            .let {
                invoiceService.saveInvoice(it, workspaceId)
            }
            .let {
                mapInvoiceDto(it, timeService)
            }
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder.executor<Invoice, InvoiceDto> {
        query(Tables.INVOICE) {
            filterByField("freeSearchText", String::class) {
                val customer = Tables.CUSTOMER.`as`("filterCustomer")
                query.join(customer).on(customer.id.eq(root.customerId))

                onPredicate(FilteringApiPredicateOperator.EQ) { filter ->
                    or(
                        root.notes.containsIgnoreCase(filter),
                        root.title.containsIgnoreCase(filter),
                        customer.name.containsIgnoreCase(filter)
                    )
                }
            }
            filterByField("status", InvoiceStatus::class) {
                onPredicate(FilteringApiPredicateOperator.EQ) { filter ->
                    when (filter) {
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
                }
            }
            addDefaultSorting { root.dateIssued.desc() }
            addDefaultSorting { root.timeRecorded.asc() }
            workspaceFilter { workspaceId ->
                // todo #222: add api to join in the root of the builder to avoid redundant joins
                val customer = Tables.CUSTOMER.`as`("customer")
                query.join(customer).on(customer.id.eq(root.customerId))
                customer.workspaceId.eq(workspaceId)
            }
        }
        mapper { mapInvoiceDto(this, timeService) }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvoiceDto(
    val title: String,
    val income: Long?,
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

private fun mapInvoiceDto(source: Invoice, timeService: TimeService) =
    InvoiceDto(
        title = source.title,
        income = source.incomeId,
        customer = source.customerId,
        timeRecorded = source.timeRecorded,
        dateIssued = source.dateIssued,
        dateCancelled = source.dateCancelled,
        datePaid = source.datePaid,
        dateSent = source.dateSent,
        dueDate = source.dueDate,
        currency = source.currency,
        amount = source.amount,
        attachments = source.attachments.map { it.documentId },
        notes = source.notes,
        id = source.id!!,
        version = source.version!!,
        status = getInvoiceStatus(source, timeService),
        generalTax = source.generalTaxId
    )

private fun getInvoiceStatus(invoice: Invoice, timeService: TimeService): InvoiceStatus {
    return when {
        invoice.dateCancelled != null -> InvoiceStatus.CANCELLED
        invoice.datePaid != null -> InvoiceStatus.PAID
        invoice.dueDate.isBefore(timeService.currentDate()) -> InvoiceStatus.OVERDUE
        invoice.dateSent != null -> InvoiceStatus.SENT
        else -> InvoiceStatus.DRAFT
    }
}
