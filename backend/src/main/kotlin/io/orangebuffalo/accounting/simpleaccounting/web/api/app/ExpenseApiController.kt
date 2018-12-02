package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.accounting.simpleaccounting.services.business.DocumentService
import io.orangebuffalo.accounting.simpleaccounting.services.business.ExpenseService
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QExpense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/v1/user/workspaces/{workspaceId}/expenses")
class ExpenseApiController(
    private val extensions: ApiControllersExtensions,
    private val expenseService: ExpenseService,
    private val documentService: DocumentService,
    private val timeService: TimeService
) {

    @PostMapping
    fun createExpense(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: CreateExpenseDto
    ): Mono<ExpenseDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId)

        expenseService.saveExpense(
            Expense(
                category = getValidCategory(workspace, request),
                title = request.title,
                timeRecorded = timeService.currentTime(),
                datePaid = request.datePaid,
                currency = request.currency,
                originalAmount = request.originalAmount,
                amountInDefaultCurrency = request.amountInDefaultCurrency ?: 0,
                actualAmountInDefaultCurrency = request.actualAmountInDefaultCurrency ?: 0,
                notes = request.notes,
                percentOnBusiness = request.percentOnBusiness ?: 100,
                attachments = getValidAttachments(request, workspace),
                reportedAmountInDefaultCurrency = 0
            )
        ).let(::mapExpenseDto)
    }

    private suspend fun getValidAttachments(
        request: CreateExpenseDto,
        workspace: Workspace
    ): List<Document> {
        val attachments = request.attachments?.let { documentService.getDocumentsByIds(it) } ?: emptyList()
        attachments.forEach { attachment ->
            if (attachment.workspace != workspace) {
                throw EntityNotFoundException("Document ${attachment.id} is not found")
            }
        }
        return attachments
    }

    private fun getValidCategory(
        workspace: Workspace,
        request: CreateExpenseDto
    ) = workspace.categories.asSequence()
        .firstOrNull { category -> category.id == request.category }
        ?: throw EntityNotFoundException("Category ${request.category} is not found")

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
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpenseDto(
    var category: Long,
    val title: String,
    var timeRecorded: Instant,
    var datePaid: LocalDate,
    var currency: String,
    var originalAmount: Long,
    var amountInDefaultCurrency: Long,
    var actualAmountInDefaultCurrency: Long,
    var reportedAmountInDefaultCurrency: Long,
    var attachments: List<Long>,
    var percentOnBusiness: Int,
    var notes: String?,
    var id: Long,
    var version: Int
)

data class CreateExpenseDto(
    val category: Long,
    val datePaid: LocalDate,
    @field:NotBlank val title: String,
    @field:NotBlank val currency: String,
    val originalAmount: Long,
    val amountInDefaultCurrency: Long?,
    val actualAmountInDefaultCurrency: Long?,
    val attachments: List<Long>?,
    val percentOnBusiness: Int?,
    @field:Size(max = 1024) val notes: String?
)

private fun mapExpenseDto(source: Expense) = ExpenseDto(
    category = source.category.id!!,
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
    version = source.version
)

class ExpensePageableApiDescriptor : PageableApiDescriptor<Expense, QExpense> {
    override fun mapEntityToDto(entity: Expense) = mapExpenseDto(entity)
}