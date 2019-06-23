package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import com.querydsl.core.types.dsl.Expressions
import io.orangebuffalo.accounting.simpleaccounting.services.business.ExpenseService
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QExpense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.toSort
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/expenses")
class ExpensesApiController(
    private val extensions: ApiControllersExtensions,
    private val expenseService: ExpenseService,
    private val timeService: TimeService
) {

    @PostMapping
    fun createExpense(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditExpenseDto
    ): Mono<ExpenseDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId)

        val tax = extensions.getValidTax(request.tax, workspace)

        expenseService.saveExpense(
            Expense(
                workspace = workspace,
                category = extensions.getValidCategory(workspace, request.category),
                title = request.title,
                timeRecorded = timeService.currentTime(),
                datePaid = request.datePaid,
                currency = request.currency,
                originalAmount = request.originalAmount,
                amountInDefaultCurrency = request.amountInDefaultCurrency ?: 0,
                actualAmountInDefaultCurrency = request.actualAmountInDefaultCurrency ?: 0,
                notes = request.notes,
                percentOnBusiness = request.percentOnBusiness ?: 100,
                attachments = extensions.getValidDocuments(workspace, request.attachments),
                reportedAmountInDefaultCurrency = 0,
                tax = tax
            )
        ).let(::mapExpenseDto)
    }

    @GetMapping
    @PageableApi(ExpensePageableApiDescriptor::class)
    fun getExpenses(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<Expense>> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        expenseService.getExpenses(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{expenseId}")
    fun getExpense(
        @PathVariable workspaceId: Long,
        @PathVariable expenseId: Long
    ): Mono<ExpenseDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val expense = expenseService.getExpenseByIdAndWorkspace(expenseId, workspace)
            ?: throw EntityNotFoundException("Expense $expenseId is not found")
        mapExpenseDto(expense)
    }

    @PutMapping("{expenseId}")
    fun updateExpense(
        @PathVariable workspaceId: Long,
        @PathVariable expenseId: Long,
        @RequestBody @Valid request: EditExpenseDto
    ): Mono<ExpenseDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId)

        // todo #71: optimistic locking. etag?
        val expense = expenseService.getExpenseByIdAndWorkspace(expenseId, workspace)
            ?: throw EntityNotFoundException("Expense $expenseId is not found")

        expense.apply {
            category = extensions.getValidCategory(workspace, request.category)
            title = request.title
            datePaid = request.datePaid
            currency = request.currency
            originalAmount = request.originalAmount
            amountInDefaultCurrency = request.amountInDefaultCurrency ?: 0
            actualAmountInDefaultCurrency = request.actualAmountInDefaultCurrency ?: 0
            notes = request.notes
            percentOnBusiness = request.percentOnBusiness ?: 100
            attachments = extensions.getValidDocuments(workspace, request.attachments)
            tax = extensions.getValidTax(request.tax, workspace)
        }.let {
            expenseService.saveExpense(it)
        }.let {
            mapExpenseDto(it)
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpenseDto(
    val category: Long?,
    val title: String,
    val timeRecorded: Instant,
    val datePaid: LocalDate,
    val currency: String,
    val originalAmount: Long,
    val amountInDefaultCurrency: Long,
    val actualAmountInDefaultCurrency: Long,
    val reportedAmountInDefaultCurrency: Long,
    val attachments: List<Long>,
    val percentOnBusiness: Int,
    val notes: String?,
    val id: Long,
    val version: Int,
    val status: ExpenseStatus,
    val tax: Long?,
    val taxRateInBps: Int?,
    val taxAmount: Long?
)

enum class ExpenseStatus {
    FINALIZED,
    PENDING_CONVERSION,
    PENDING_ACTUAL_RATE
}

data class EditExpenseDto(
    val category: Long?,
    val datePaid: LocalDate,
    @field:NotBlank val title: String,
    @field:NotBlank val currency: String,
    val originalAmount: Long,
    val amountInDefaultCurrency: Long?,
    val actualAmountInDefaultCurrency: Long?,
    val attachments: List<Long>?,
    val percentOnBusiness: Int?,
    @field:Size(max = 1024) val notes: String?,
    val tax: Long?
)

private fun mapExpenseDto(source: Expense) = ExpenseDto(
    category = source.category?.id,
    title = source.title,
    datePaid = source.datePaid,
    timeRecorded = source.timeRecorded,
    currency = source.currency,
    originalAmount = source.originalAmount,
    amountInDefaultCurrency = source.amountInDefaultCurrency,
    actualAmountInDefaultCurrency = source.actualAmountInDefaultCurrency,
    attachments = source.attachments.map { it.id!! },
    percentOnBusiness = source.percentOnBusiness,
    reportedAmountInDefaultCurrency = source.reportedAmountInDefaultCurrency,
    notes = source.notes,
    id = source.id!!,
    version = source.version,
    status = getExpenseStatus(source),
    tax = source.tax?.id,
    taxAmount = source.taxAmount,
    taxRateInBps = source.taxRateInBps
)

private fun getExpenseStatus(expense: Expense): ExpenseStatus {
    return when {
        expense.reportedAmountInDefaultCurrency > 0 -> ExpenseStatus.FINALIZED
        expense.amountInDefaultCurrency > 0 -> ExpenseStatus.PENDING_ACTUAL_RATE
        else -> ExpenseStatus.PENDING_CONVERSION
    }
}

@Component
class ExpensePageableApiDescriptor : PageableApiDescriptor<Expense, QExpense> {
    override suspend fun mapEntityToDto(entity: Expense) = mapExpenseDto(entity)

    override fun getSupportedFilters() = apiFilters(QExpense.expense) {
        byApiField("freeSearchText", String::class) {
            onOperator(PageableApiFilterOperator.EQ) { value ->
                Expressions.anyOf(
                    notes.containsIgnoreCase(value),
                    title.containsIgnoreCase(value),
                    category.name.containsIgnoreCase(value)
                )
            }
        }

        byApiField("status", ExpenseStatus::class) {
            onOperator(PageableApiFilterOperator.EQ) { value ->
                when (value) {
                    ExpenseStatus.FINALIZED -> reportedAmountInDefaultCurrency.gt(0)
                    ExpenseStatus.PENDING_ACTUAL_RATE -> reportedAmountInDefaultCurrency.eq(0)
                        .and(amountInDefaultCurrency.gt(0))
                    ExpenseStatus.PENDING_CONVERSION -> reportedAmountInDefaultCurrency.eq(0)
                        .and(amountInDefaultCurrency.eq(0))
                }
            }
        }
    }

    override fun getDefaultSorting(): Sort = QExpense.expense.datePaid.desc().toSort()
}