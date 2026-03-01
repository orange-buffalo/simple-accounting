package io.orangebuffalo.simpleaccounting.business.expenses

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.ApiPageRequest
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.NoOpSorting
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
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
class ExpensesApi(
    private val expenseService: ExpenseService,
    private val workspacesService: WorkspacesService,
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
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
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
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)

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
                addDefaultSorting { root.createdAt.asc() }
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
    val createdAt: Instant,
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
    createdAt = createdAt!!,
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
