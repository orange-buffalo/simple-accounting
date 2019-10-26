package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import com.querydsl.core.types.dsl.Expressions
import io.orangebuffalo.accounting.simpleaccounting.services.business.*
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QIncome
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.toSort
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.*
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
@RequestMapping("/api/workspaces/{workspaceId}/incomes")
class IncomesApiController(
    private val extensions: ApiControllersExtensions,
    private val incomeService: IncomeService,
    private val timeService: TimeService,
    private val invoiceService: InvoiceService,
    private val workspaceService: WorkspaceService
) {

    @PostMapping
    suspend fun createIncome(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditIncomeDto
    ): IncomeDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        return incomeService
            .saveIncome(
                Income(
                    workspace = workspace,
                    category = extensions.getValidCategory(workspace, request.category),
                    title = request.title,
                    timeRecorded = timeService.currentTime(),
                    dateReceived = request.dateReceived,
                    currency = request.currency,
                    originalAmount = request.originalAmount,
                    amountInDefaultCurrency = request.amountInDefaultCurrency ?: 0,
                    reportedAmountInDefaultCurrency = request.reportedAmountInDefaultCurrency ?: 0,
                    notes = request.notes,
                    attachments = extensions.getValidDocuments(workspace, request.attachments),
                    tax = extensions.getValidTax(request.tax, workspace)
                )
            )
            .let { mapIncomeDto(it, invoiceService) }
    }

    @GetMapping
    @PageableApi(IncomePageableApiDescriptor::class)
    suspend fun getIncomes(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Page<Income> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        return incomeService.getIncomes(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{incomeId}")
    suspend fun getIncome(
        @PathVariable workspaceId: Long,
        @PathVariable incomeId: Long
    ): IncomeDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val income = incomeService.getIncomeByIdAndWorkspace(incomeId, workspace)
            ?: throw EntityNotFoundException("Income $incomeId is not found")
        return mapIncomeDto(income, invoiceService)
    }

    @PutMapping("{incomeId}")
    suspend fun updateIncome(
        @PathVariable workspaceId: Long,
        @PathVariable incomeId: Long,
        @RequestBody @Valid request: EditIncomeDto
    ): IncomeDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val income = incomeService.getIncomeByIdAndWorkspace(incomeId, workspace)
            ?: throw EntityNotFoundException("Income $incomeId is not found")

        return income
            .apply {
                category = extensions.getValidCategory(workspace, request.category)
                title = request.title
                dateReceived = request.dateReceived
                currency = request.currency
                originalAmount = request.originalAmount
                amountInDefaultCurrency = request.amountInDefaultCurrency ?: 0
                reportedAmountInDefaultCurrency = request.reportedAmountInDefaultCurrency ?: 0
                notes = request.notes
                attachments = extensions.getValidDocuments(workspace, request.attachments)
                tax = extensions.getValidTax(request.tax, workspace)
            }
            .let {
                incomeService.saveIncome(it)
            }
            .let {
                mapIncomeDto(it, invoiceService)
            }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class IncomeDto(
    val category: Long?,
    val title: String,
    val timeRecorded: Instant,
    val dateReceived: LocalDate,
    val currency: String,
    val originalAmount: Long,
    val amountInDefaultCurrency: Long,
    val reportedAmountInDefaultCurrency: Long,
    val attachments: List<Long>,
    val notes: String?,
    val id: Long,
    val version: Int,
    val status: IncomeStatus,
    val linkedInvoice: LinkedInvoiceDto?,
    val tax: Long?,
    val taxRateInBps: Int?,
    val taxAmount: Long?
)

data class LinkedInvoiceDto(
    val id: Long,
    val title: String
)

enum class IncomeStatus {
    FINALIZED,
    PENDING_CONVERSION,
    PENDING_ACTUAL_RATE
}

data class EditIncomeDto(
    val category: Long?,
    val dateReceived: LocalDate,
    @field:NotBlank val title: String,
    @field:NotBlank val currency: String,
    val originalAmount: Long,
    val amountInDefaultCurrency: Long?,
    val reportedAmountInDefaultCurrency: Long?,
    val attachments: List<Long>?,
    @field:Size(max = 1024) val notes: String?,
    val tax: Long?
)

private suspend fun mapIncomeDto(source: Income, invoiceService: InvoiceService): IncomeDto {
    val linkedInvoice: Invoice? = invoiceService.findByIncome(source)
    return IncomeDto(
        category = source.category?.id,
        title = source.title,
        dateReceived = source.dateReceived,
        timeRecorded = source.timeRecorded,
        currency = source.currency,
        originalAmount = source.originalAmount,
        amountInDefaultCurrency = source.amountInDefaultCurrency,
        attachments = source.attachments.map { it.id!! },
        reportedAmountInDefaultCurrency = source.reportedAmountInDefaultCurrency,
        notes = source.notes,
        id = source.id!!,
        version = source.version,
        status = getIncomeStatus(source),
        linkedInvoice = linkedInvoice?.let {
            LinkedInvoiceDto(
                id = it.id!!,
                title = it.title
            )
        },
        tax = source.tax?.id,
        taxAmount = source.taxAmount,
        taxRateInBps = source.taxRateInBps
    )
}

private fun getIncomeStatus(income: Income): IncomeStatus {
    return when {
        income.reportedAmountInDefaultCurrency > 0 -> IncomeStatus.FINALIZED
        income.amountInDefaultCurrency > 0 -> IncomeStatus.PENDING_ACTUAL_RATE
        else -> IncomeStatus.PENDING_CONVERSION
    }
}

@Component
class IncomePageableApiDescriptor(
    private val invoiceService: InvoiceService
) : PageableApiDescriptor<Income, QIncome> {

    override suspend fun mapEntityToDto(entity: Income) = mapIncomeDto(entity, invoiceService)

    override fun getSupportedFilters() = apiFilters(QIncome.income) {
        byApiField("freeSearchText", String::class) {
            onOperator(PageableApiFilterOperator.EQ) { value ->
                Expressions.anyOf(
                    notes.containsIgnoreCase(value),
                    title.containsIgnoreCase(value),
                    category.name.containsIgnoreCase(value)
                )
            }
        }

        byApiField("status", IncomeStatus::class) {
            onOperator(PageableApiFilterOperator.EQ) { value ->
                when (value) {
                    IncomeStatus.FINALIZED -> reportedAmountInDefaultCurrency.gt(0)
                    IncomeStatus.PENDING_ACTUAL_RATE -> reportedAmountInDefaultCurrency.eq(0)
                        .and(amountInDefaultCurrency.gt(0))
                    IncomeStatus.PENDING_CONVERSION -> reportedAmountInDefaultCurrency.eq(0)
                        .and(amountInDefaultCurrency.eq(0))
                }
            }
        }
    }

    override fun getDefaultSorting(): Sort = QIncome.income.dateReceived.desc().toSort()
}