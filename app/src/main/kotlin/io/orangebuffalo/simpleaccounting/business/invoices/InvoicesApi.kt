package io.orangebuffalo.simpleaccounting.business.invoices

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.ApiPageRequest
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.NoOpSorting
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.swagger.v3.oas.annotations.Parameter
import org.hibernate.validator.constraints.Length
import org.jooq.impl.DSL.or
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springdoc.core.annotations.ParameterObject

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/invoices")
class InvoicesApi(
    private val invoicesService: InvoicesService,
    private val workspacesService: WorkspacesService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @PostMapping
    suspend fun createInvoice(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditInvoiceDto
    ): InvoiceDto = invoicesService
        .saveInvoice(
            Invoice(
                title = request.title,
                customerId = request.customer,
                currency = request.currency,
                notes = request.notes,
                attachments = getInvoiceAttachments(request.attachments),
                dateIssued = request.dateIssued,
                dateSent = request.dateSent,
                datePaid = request.datePaid,
                dueDate = request.dueDate,
                amount = request.amount,
                generalTaxId = request.generalTax
            ),
            workspaceId
        )
        .mapToInvoiceDto()

    private fun getInvoiceAttachments(attachments: List<Long>?): Set<InvoiceAttachment> =
        attachments?.asSequence()?.map(::InvoiceAttachment)?.toSet() ?: emptySet()

    @GetMapping
    suspend fun getInvoices(
        @PathVariable workspaceId: Long,
        @ParameterObject request: InvoicesFilteringRequest
    ) = filteringApiExecutor.executeFiltering(request, workspaceId)

    @GetMapping("{invoiceId}")
    suspend fun getInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long
    ): InvoiceDto {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        // todo #71: when optimistic locking is addressed, move access control into the business service
        val invoice = invoicesService.getInvoiceByIdAndWorkspaceId(invoiceId, workspaceId)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")
        return invoice.mapToInvoiceDto()
    }

    @PutMapping("{invoiceId}")
    suspend fun updateInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long,
        @RequestBody @Valid request: EditInvoiceDto
    ): InvoiceDto {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val invoice = invoicesService.getInvoiceByIdAndWorkspaceId(invoiceId, workspaceId)
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
                dueDate = request.dueDate
                amount = request.amount
                generalTaxId = request.generalTax
            }
            .let { invoicesService.saveInvoice(it, workspaceId) }
            .mapToInvoiceDto()
    }

    @PostMapping("{invoiceId}/cancel")
    suspend fun cancelInvoice(
        @PathVariable workspaceId: Long,
        @PathVariable invoiceId: Long
    ): InvoiceDto {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)
        return invoicesService.cancelInvoice(invoiceId, workspaceId)
            .mapToInvoiceDto()
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder
        .executor<Invoice, InvoiceDto, NoOpSorting, InvoicesFilteringRequest> {
            query(Tables.INVOICE) {
                val customer = Tables.CUSTOMER.`as`("filterCustomer")

                configure {
                    query.join(customer).on(customer.id.eq(root.customerId))
                }

                onFilter(InvoicesFilteringRequest::freeSearchText) { filter ->
                    or(
                        root.notes.containsIgnoreCase(filter),
                        root.title.containsIgnoreCase(filter),
                        customer.name.containsIgnoreCase(filter)
                    )
                }

                onFilter(InvoicesFilteringRequest::statusIn) { statuses -> root.status.`in`(statuses) }

                addDefaultSorting { root.dateIssued.desc() }
                addDefaultSorting { root.createdAt.asc() }
                workspaceFilter { workspaceId -> customer.workspaceId.eq(workspaceId) }
            }
            mapper { mapToInvoiceDto() }
        }
}

class InvoicesFilteringRequest : ApiPageRequest<NoOpSorting>() {
    override var sortBy: NoOpSorting? = null

    @field:Parameter(name = "freeSearchText[eq]")
    var freeSearchText: String? = null

    @field:Parameter(name = "status[in]")
    var statusIn: List<InvoiceStatus>? = null
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvoiceDto(
    val title: String,
    val customer: Long,
    val createdAt: Instant,
    val dateIssued: LocalDate,
    val dateSent: LocalDate?,
    val datePaid: LocalDate?,
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

private fun Invoice.mapToInvoiceDto() = InvoiceDto(
    title = title,
    customer = customerId,
    createdAt = createdAt!!,
    dateIssued = dateIssued,
    datePaid = datePaid,
    dateSent = dateSent,
    dueDate = dueDate,
    currency = currency,
    amount = amount,
    attachments = attachments.map { it.documentId },
    notes = notes,
    id = id!!,
    version = version!!,
    status = status,
    generalTax = generalTaxId
)
