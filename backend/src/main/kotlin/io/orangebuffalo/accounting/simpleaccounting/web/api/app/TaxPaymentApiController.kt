package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.accounting.simpleaccounting.services.business.TaxPaymentService
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QTaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.TaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.toSort
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.hibernate.validator.constraints.Length
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tax-payments")
class TaxPaymentApiController(
    private val extensions: ApiControllersExtensions,
    private val taxPaymentService: TaxPaymentService,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService
) {

    @PostMapping
    suspend fun createTaxPayment(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditTaxPaymentDto
    ): TaxPaymentDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        return taxPaymentService
            .saveTaxPayment(
                TaxPayment(
                    timeRecorded = timeService.currentTime(),
                    datePaid = request.datePaid,
                    reportingDate = request.reportingDate ?: request.datePaid,
                    notes = request.notes,
                    attachments = extensions.getValidDocuments(workspace, request.attachments),
                    amount = request.amount,
                    workspace = workspace,
                    title = request.title
                )
            )
            .let(::mapTaxPaymentDto)
    }

    @GetMapping
    @PageableApi(TaxPaymentPageableApiDescriptor::class)
    suspend fun getTaxPayments(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Page<TaxPayment> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        return taxPaymentService.getTaxPayments(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{taxPaymentId}")
    suspend fun getTaxPayment(
        @PathVariable workspaceId: Long,
        @PathVariable taxPaymentId: Long
    ): TaxPaymentDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val taxPayment = taxPaymentService.getTaxPaymentByIdAndWorkspace(taxPaymentId, workspace)
            ?: throw EntityNotFoundException("Tax Payment $taxPaymentId is not found")
        return mapTaxPaymentDto(taxPayment)
    }

    @PutMapping("{taxPaymentId}")
    suspend fun updateTaxPayment(
        @PathVariable workspaceId: Long,
        @PathVariable taxPaymentId: Long,
        @RequestBody @Valid request: EditTaxPaymentDto
    ): TaxPaymentDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val income = taxPaymentService.getTaxPaymentByIdAndWorkspace(taxPaymentId, workspace)
            ?: throw EntityNotFoundException("Tax Payment $taxPaymentId is not found")

        return income
            .apply {
                notes = request.notes
                attachments = extensions.getValidDocuments(workspace, request.attachments)
                datePaid = request.datePaid
                reportingDate = request.reportingDate ?: request.datePaid
                amount = request.amount
                title = request.title
            }
            .let {
                taxPaymentService.saveTaxPayment(it)
            }
            .let {
                mapTaxPaymentDto(it)
            }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TaxPaymentDto(
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

data class EditTaxPaymentDto(
    val datePaid: LocalDate,
    val reportingDate: LocalDate?,
    val amount: Long,
    val attachments: List<Long>,
    @field:Length(max = 1024) val notes: String?,
    @field:NotBlank @field:Length(max = 255) val title: String
)

private fun mapTaxPaymentDto(source: TaxPayment) = TaxPaymentDto(
    id = source.id!!,
    version = source.version,
    timeRecorded = source.timeRecorded,
    datePaid = source.datePaid,
    reportingDate = source.reportingDate,
    amount = source.amount,
    attachments = source.attachments.map { it.id!! },
    notes = source.notes,
    title = source.title
)

@Component
class TaxPaymentPageableApiDescriptor : PageableApiDescriptor<TaxPayment, QTaxPayment> {
    override suspend fun mapEntityToDto(entity: TaxPayment) = mapTaxPaymentDto(entity)

    override fun getDefaultSorting(): Sort = QTaxPayment.taxPayment.datePaid.desc().toSort()
}