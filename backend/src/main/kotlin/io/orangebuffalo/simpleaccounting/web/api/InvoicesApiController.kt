package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.querydsl.core.types.dsl.Expressions
import io.orangebuffalo.simpleaccounting.services.business.InvoiceService
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.simpleaccounting.services.persistence.entities.QInvoice
import io.orangebuffalo.simpleaccounting.services.persistence.toSort
import io.orangebuffalo.simpleaccounting.web.api.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.web.api.integration.*
import org.hibernate.validator.constraints.Length
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/invoices")
class InvoicesApiController(
    private val extensions: ApiControllersExtensions,
    private val invoiceService: InvoiceService,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService
) {

    @PostMapping
    suspend fun createInvoice(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditInvoiceDto
    ): InvoiceDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        return invoiceService
            .saveInvoice(
                Invoice(
                    title = request.title,
                    timeRecorded = timeService.currentTime(),
                    customer = extensions.getValidCustomer(workspace, request.customer),
                    currency = request.currency,
                    notes = request.notes,
                    attachments = extensions.getValidDocuments(workspace, request.attachments),
                    dateIssued = request.dateIssued,
                    dateSent = request.dateSent,
                    datePaid = request.datePaid,
                    dateCancelled = request.dateCancelled,
                    dueDate = request.dueDate,
                    amount = request.amount,
                    generalTax = extensions.getValidGeneralTax(request.generalTax, workspace)
                )
            )
            .let { mapInvoiceDto(it, timeService) }
    }

    @GetMapping
    @PageableApi(InvoicePageableApiDescriptor::class)
    suspend fun getInvoices(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Page<Invoice> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        return invoiceService.getInvoices(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{invoiceId}")
    suspend fun getInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long
    ): InvoiceDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val income = invoiceService.getInvoiceByIdAndWorkspace(invoiceId, workspace)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")
        return mapInvoiceDto(income, timeService)
    }

    @PutMapping("{invoiceId}")
    suspend fun updateInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long,
        @RequestBody @Valid request: EditInvoiceDto
    ): InvoiceDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val income = invoiceService.getInvoiceByIdAndWorkspace(invoiceId, workspace)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")

        return income
            .apply {
                title = request.title
                customer = extensions.getValidCustomer(workspace, request.customer)
                currency = request.currency
                notes = request.notes
                attachments = extensions.getValidDocuments(workspace, request.attachments)
                dateIssued = request.dateIssued
                dateSent = request.dateSent
                datePaid = request.datePaid
                dateCancelled = request.dateCancelled
                dueDate = request.dueDate
                amount = request.amount
                generalTax = extensions.getValidGeneralTax(request.generalTax, workspace)
            }
            .let {
                invoiceService.saveInvoice(it)
            }
            .let {
                mapInvoiceDto(it, timeService)
            }
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
        income = source.income?.id,
        customer = source.customer.id!!,
        timeRecorded = source.timeRecorded,
        dateIssued = source.dateIssued,
        dateCancelled = source.dateCancelled,
        datePaid = source.datePaid,
        dateSent = source.dateSent,
        dueDate = source.dueDate,
        currency = source.currency,
        amount = source.amount,
        attachments = source.attachments.map { it.id!! },
        notes = source.notes,
        id = source.id!!,
        version = source.version,
        status = getInvoiceStatus(source, timeService),
        generalTax = source.generalTax?.id
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

@Component
class InvoicePageableApiDescriptor(
    private val timeService: TimeService
) : PageableApiDescriptor<Invoice, QInvoice> {

    override suspend fun mapEntityToDto(entity: Invoice) =
        mapInvoiceDto(entity, timeService)

    override fun getSupportedFilters() = apiFilters(QInvoice.invoice) {
        byApiField("freeSearchText", String::class) {
            onOperator(PageableApiFilterOperator.EQ) { value ->
                Expressions.anyOf(
                    notes.containsIgnoreCase(value),
                    title.containsIgnoreCase(value),
                    customer.name.containsIgnoreCase(value)
                )
            }
        }

        byApiField("status", InvoiceStatus::class) {
            onOperator(PageableApiFilterOperator.EQ) { value ->
                when (value) {
                    InvoiceStatus.DRAFT -> datePaid.isNull.and(dateSent.isNull).and(dateCancelled.isNull)
                    InvoiceStatus.CANCELLED -> dateCancelled.isNotNull
                    InvoiceStatus.PAID -> datePaid.isNotNull.and(dateCancelled.isNull)
                    InvoiceStatus.SENT -> dateSent.isNotNull.and(datePaid.isNull).and(dateCancelled.isNull)
                        .and(dueDate.goe(timeService.currentDate()))
                    InvoiceStatus.OVERDUE -> datePaid.isNull.and(dateCancelled.isNull)
                        .and(dueDate.lt(timeService.currentDate()))
                }
            }
        }
    }

    override fun getDefaultSorting(): Sort = QInvoice.invoice.dateIssued.desc().toSort()
}
