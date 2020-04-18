package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.querydsl.core.types.dsl.Expressions
import io.orangebuffalo.simpleaccounting.services.business.ExpenseService
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.LegacyAmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.simpleaccounting.services.persistence.entities.ExpenseStatus
import io.orangebuffalo.simpleaccounting.services.persistence.entities.QExpense
import io.orangebuffalo.simpleaccounting.services.persistence.toOrder
import io.orangebuffalo.simpleaccounting.web.api.integration.*
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
@RequestMapping("/api/workspaces/{workspaceId}/expenses")
class ExpensesApiController(
    private val extensions: ApiControllersExtensions,
    private val expenseService: ExpenseService,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService
) {

    @PostMapping
    suspend fun createExpense(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditExpenseDto
    ): ExpenseDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        val generalTax = extensions.getValidGeneralTax(request.generalTax, workspace)

        return expenseService
            .saveExpense(
                Expense(
                    workspace = workspace,
                    category = extensions.getValidCategory(workspace, request.category),
                    title = request.title,
                    timeRecorded = timeService.currentTime(),
                    datePaid = request.datePaid,
                    currency = request.currency,
                    originalAmount = request.originalAmount,
                    convertedAmounts = LegacyAmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = request.convertedAmountInDefaultCurrency,
                        adjustedAmountInDefaultCurrency = null
                    ),
                    incomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = request.incomeTaxableAmountInDefaultCurrency,
                        adjustedAmountInDefaultCurrency = null
                    ),
                    useDifferentExchangeRateForIncomeTaxPurposes = request.useDifferentExchangeRateForIncomeTaxPurposes,
                    notes = request.notes,
                    percentOnBusiness = request.percentOnBusiness ?: 100,
                    attachments = extensions.getValidDocuments(workspace, request.attachments),
                    generalTax = generalTax,
                    status = ExpenseStatus.PENDING_CONVERSION
                )
            )
            .let(Expense::mapToExpenseDto)
    }

    @GetMapping
    @PageableApi(ExpensePageableApiDescriptor::class)
    suspend fun getExpenses(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Page<Expense> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        return expenseService.getExpenses(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{expenseId}")
    suspend fun getExpense(
        @PathVariable workspaceId: Long,
        @PathVariable expenseId: Long
    ): ExpenseDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expense = expenseService.getExpenseByIdAndWorkspace(expenseId, workspace)
            ?: throw EntityNotFoundException("Expense $expenseId is not found")
        return expense.mapToExpenseDto()
    }

    @PutMapping("{expenseId}")
    suspend fun updateExpense(
        @PathVariable workspaceId: Long,
        @PathVariable expenseId: Long,
        @RequestBody @Valid request: EditExpenseDto
    ): ExpenseDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val expense = expenseService.getExpenseByIdAndWorkspace(expenseId, workspace)
            ?: throw EntityNotFoundException("Expense $expenseId is not found")

        return expense
            .apply {
                category = extensions.getValidCategory(workspace, request.category)
                title = request.title
                datePaid = request.datePaid
                currency = request.currency
                originalAmount = request.originalAmount
                convertedAmounts.originalAmountInDefaultCurrency = request.convertedAmountInDefaultCurrency
                incomeTaxableAmounts.originalAmountInDefaultCurrency = request.incomeTaxableAmountInDefaultCurrency
                useDifferentExchangeRateForIncomeTaxPurposes = request.useDifferentExchangeRateForIncomeTaxPurposes
                notes = request.notes
                percentOnBusiness = request.percentOnBusiness ?: 100
                attachments = extensions.getValidDocuments(workspace, request.attachments)
                generalTax = extensions.getValidGeneralTax(request.generalTax, workspace)
            }
            .let { expenseService.saveExpense(it) }
            .mapToExpenseDto()
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
    val attachments: List<Long>,
    val percentOnBusiness: Int,
    val notes: String?,
    val id: Long,
    val version: Int,
    val status: ExpenseStatus,
    val generalTax: Long?,
    val generalTaxRateInBps: Int?,
    val generalTaxAmount: Long?,
    val convertedAmounts: ExpenseAmountsDto,
    val incomeTaxableAmounts: ExpenseAmountsDto,
    val useDifferentExchangeRateForIncomeTaxPurposes: Boolean
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpenseAmountsDto(
    val originalAmountInDefaultCurrency: Long?,
    val adjustedAmountInDefaultCurrency: Long?
)

data class EditExpenseDto(
    val category: Long?,
    val datePaid: LocalDate,
    @field:NotBlank val title: String,
    @field:NotBlank val currency: String,
    val originalAmount: Long,
    val convertedAmountInDefaultCurrency: Long?,
    val useDifferentExchangeRateForIncomeTaxPurposes: Boolean,
    val incomeTaxableAmountInDefaultCurrency: Long?,
    val attachments: List<Long>?,
    val percentOnBusiness: Int?,
    @field:Size(max = 1024) val notes: String?,
    val generalTax: Long?
)

private fun Expense.mapToExpenseDto() = ExpenseDto(
    category = category?.id,
    title = title,
    datePaid = datePaid,
    timeRecorded = timeRecorded,
    currency = currency,
    originalAmount = originalAmount,
    convertedAmounts = convertedAmounts.mapToAmountsDto(),
    useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
    incomeTaxableAmounts = incomeTaxableAmounts.mapToAmountsDto(),
    attachments = attachments.map { it.id!! },
    percentOnBusiness = percentOnBusiness,
    notes = notes,
    id = id!!,
    version = version,
    status = status,
    generalTax = generalTax?.id,
    generalTaxAmount = generalTaxAmount,
    generalTaxRateInBps = generalTaxRateInBps
)

private fun LegacyAmountsInDefaultCurrency.mapToAmountsDto() =
    ExpenseAmountsDto(
        originalAmountInDefaultCurrency = originalAmountInDefaultCurrency,
        adjustedAmountInDefaultCurrency = adjustedAmountInDefaultCurrency
    )

@Component
class ExpensePageableApiDescriptor : PageableApiDescriptor<Expense, QExpense> {
    override suspend fun mapEntityToDto(entity: Expense) = entity.mapToExpenseDto()

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
    }

    override fun getDefaultSorting() = Sort.by(
        QExpense.expense.datePaid.desc().toOrder(),
        QExpense.expense.timeRecorded.asc().toOrder()
    )
}
