package io.orangebuffalo.simpleaccounting.domain.expenses

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.*
import io.swagger.v3.oas.annotations.Parameter
import org.jooq.impl.DSL.or
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/expenses")
class ExpensesApiController(
    private val expenseService: ExpenseService,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @PostMapping
    suspend fun createExpense(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditExpenseDto
    ): ExpenseDto = expenseService
        .saveExpense(
            Expense(
                workspaceId = workspaceId,
                categoryId = request.category,
                title = request.title,
                timeRecorded = timeService.currentTime(),
                datePaid = request.datePaid,
                currency = request.currency,
                originalAmount = request.originalAmount,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = request.convertedAmountInDefaultCurrency,
                    adjustedAmountInDefaultCurrency = null
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = request.incomeTaxableAmountInDefaultCurrency,
                    adjustedAmountInDefaultCurrency = null
                ),
                useDifferentExchangeRateForIncomeTaxPurposes = request.useDifferentExchangeRateForIncomeTaxPurposes,
                notes = request.notes,
                percentOnBusiness = request.percentOnBusiness ?: 100,
                attachments = toExpenseAttachments(request.attachments),
                generalTaxId = request.generalTax,
                status = ExpenseStatus.PENDING_CONVERSION
            )
        )
        .mapToExpenseDto()

    private fun toExpenseAttachments(documentIds: Collection<Long>?) =
        documentIds?.asSequence()?.map(::ExpenseAttachment)?.toSet() ?: emptySet()

    @GetMapping
    suspend fun getExpenses(
        @PathVariable workspaceId: Long,
        @ParameterObject request: ExpensesFilteringRequest
    ): ApiPage<ExpenseDto> = filteringApiExecutor.executeFiltering(request, workspaceId)

    @GetMapping("{expenseId}")
    suspend fun getExpense(
        @PathVariable workspaceId: Long,
        @PathVariable expenseId: Long
    ): ExpenseDto {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expense = expenseService.getExpenseByIdAndWorkspace(expenseId, workspaceId)
            ?: throw EntityNotFoundException("Expense $expenseId is not found")
        return expense.mapToExpenseDto()
    }

    @PutMapping("{expenseId}")
    @Suppress("DuplicatedCode")
    suspend fun updateExpense(
        @PathVariable workspaceId: Long,
        @PathVariable expenseId: Long,
        @RequestBody @Valid request: EditExpenseDto
    ): ExpenseDto {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val expense = expenseService.getExpenseByIdAndWorkspace(expenseId, workspaceId)
            ?: throw EntityNotFoundException("Expense $expenseId is not found")

        return expense
            .apply {
                categoryId = request.category
                title = request.title
                datePaid = request.datePaid
                currency = request.currency
                originalAmount = request.originalAmount
                convertedAmounts.originalAmountInDefaultCurrency = request.convertedAmountInDefaultCurrency
                incomeTaxableAmounts.originalAmountInDefaultCurrency = request.incomeTaxableAmountInDefaultCurrency
                useDifferentExchangeRateForIncomeTaxPurposes = request.useDifferentExchangeRateForIncomeTaxPurposes
                notes = request.notes
                percentOnBusiness = request.percentOnBusiness ?: 100
                attachments = toExpenseAttachments(request.attachments)
                generalTaxId = request.generalTax
            }
            .let { expenseService.saveExpense(it) }
            .mapToExpenseDto()
    }

    private val filteringApiExecutor =
        filteringApiExecutorBuilder.executor<Expense, ExpenseDto, NoOpSorting, ExpensesFilteringRequest> {
            query(Tables.EXPENSE) {
                val category = Tables.CATEGORY.`as`("filterCategory")
                configure {
                    query.leftJoin(category).on(category.id.eq(root.categoryId))
                }

                onFilter(ExpensesFilteringRequest::freeSearchText) { searchString ->
                    or(
                        root.notes.containsIgnoreCase(searchString),
                        root.title.containsIgnoreCase(searchString),
                        category.name.containsIgnoreCase(searchString)
                    )
                }
                addDefaultSorting { root.datePaid.desc() }
                addDefaultSorting { root.timeRecorded.asc() }
                workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
            }
            mapper { mapToExpenseDto() }
        }
}

class ExpensesFilteringRequest : ApiPageRequest<NoOpSorting>() {
    override var sortBy: NoOpSorting? = null

    @field:Parameter(name = "freeSearchText[eq]")
    var freeSearchText: String? = null
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
    category = categoryId,
    title = title,
    datePaid = datePaid,
    timeRecorded = timeRecorded,
    currency = currency,
    originalAmount = originalAmount,
    convertedAmounts = convertedAmounts.mapToAmountsDto(),
    useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
    incomeTaxableAmounts = incomeTaxableAmounts.mapToAmountsDto(),
    attachments = attachments.map { it.documentId },
    percentOnBusiness = percentOnBusiness,
    notes = notes,
    id = id!!,
    version = version!!,
    status = status,
    generalTax = generalTaxId,
    generalTaxAmount = generalTaxAmount,
    generalTaxRateInBps = generalTaxRateInBps
)

private fun AmountsInDefaultCurrency.mapToAmountsDto() = ExpenseAmountsDto(
    originalAmountInDefaultCurrency = originalAmountInDefaultCurrency,
    adjustedAmountInDefaultCurrency = adjustedAmountInDefaultCurrency
)
