package io.orangebuffalo.simpleaccounting.domain.incometaxpayments

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilderLegacy
import org.hibernate.validator.constraints.Length
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/income-tax-payments")
class IncomeTaxPaymentsApiController(
    private val taxPaymentService: IncomeTaxPaymentService,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilderLegacy
) {

    @PostMapping
    suspend fun createTaxPayment(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditIncomeTaxPaymentDto
    ): IncomeTaxPaymentDto = taxPaymentService
        .saveTaxPayment(
            IncomeTaxPayment(
                timeRecorded = timeService.currentTime(),
                datePaid = request.datePaid,
                reportingDate = request.reportingDate ?: request.datePaid,
                notes = request.notes,
                attachments = mapAttachments(request.attachments),
                amount = request.amount,
                workspaceId = workspaceId,
                title = request.title
            )
        )
        .mapToIncomeTaxPaymentDto()

    private fun mapAttachments(attachmentsIds: Collection<Long>?): Set<IncomeTaxPaymentAttachment> =
        attachmentsIds?.asSequence()?.map(::IncomeTaxPaymentAttachment)?.toSet() ?: emptySet()

    @GetMapping
    suspend fun getTaxPayments(@PathVariable workspaceId: Long): ApiPage<IncomeTaxPaymentDto> =
        filteringApiExecutor.executeFiltering(workspaceId)

    @GetMapping("{taxPaymentId}")
    suspend fun getTaxPayment(
        @PathVariable workspaceId: Long,
        @PathVariable taxPaymentId: Long
    ): IncomeTaxPaymentDto {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val taxPayment = taxPaymentService.getTaxPaymentByIdAndWorkspace(taxPaymentId, workspaceId)
            ?: throw EntityNotFoundException("Income Tax Payment $taxPaymentId is not found")
        return taxPayment.mapToIncomeTaxPaymentDto()
    }

    @PutMapping("{taxPaymentId}")
    suspend fun updateTaxPayment(
        @PathVariable workspaceId: Long,
        @PathVariable taxPaymentId: Long,
        @RequestBody @Valid request: EditIncomeTaxPaymentDto
    ): IncomeTaxPaymentDto {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val income = taxPaymentService.getTaxPaymentByIdAndWorkspace(taxPaymentId, workspaceId)
            ?: throw EntityNotFoundException("Income Tax Payment $taxPaymentId is not found")

        return income
            .apply {
                notes = request.notes
                attachments = mapAttachments(request.attachments)
                datePaid = request.datePaid
                reportingDate = request.reportingDate ?: request.datePaid
                amount = request.amount
                title = request.title
            }
            .let { taxPaymentService.saveTaxPayment(it) }
            .mapToIncomeTaxPaymentDto()
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder.executor<IncomeTaxPayment, IncomeTaxPaymentDto> {
        query(Tables.INCOME_TAX_PAYMENT) {
            addDefaultSorting { root.datePaid.desc() }
            addDefaultSorting { root.timeRecorded.asc() }
            workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
        }
        mapper { mapToIncomeTaxPaymentDto() }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class IncomeTaxPaymentDto(
    val id: Long,
    val version: Int,
    val title: String,
    val timeRecorded: Instant,
    val datePaid: LocalDate,
    val reportingDate: LocalDate,
    val amount: Long,
    val attachments: List<Long>,
    val notes: String?
)

data class EditIncomeTaxPaymentDto(
    val datePaid: LocalDate,
    val reportingDate: LocalDate?,
    val amount: Long,
    val attachments: List<Long>?,
    @field:Length(max = 1024) val notes: String?,
    @field:NotBlank @field:Length(max = 255) val title: String
)

private fun IncomeTaxPayment.mapToIncomeTaxPaymentDto() = IncomeTaxPaymentDto(
    id = id!!,
    version = version!!,
    timeRecorded = timeRecorded,
    datePaid = datePaid,
    reportingDate = reportingDate,
    amount = amount,
    attachments = attachments.map { it.documentId },
    notes = notes,
    title = title
)
