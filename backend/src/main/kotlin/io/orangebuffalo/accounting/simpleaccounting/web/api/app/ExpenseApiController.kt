package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.DocumentService
import io.orangebuffalo.accounting.simpleaccounting.services.business.ExpenseService
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiDto
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.mapping.ApiDtoMapperAdapter
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
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
        @RequestBody requestMono: Mono<CreateExpenseDto>
    ): Mono<ExpenseDto> = extensions.withAccessibleWorkspace(workspaceId) { workspace ->

        requestMono
            .flatMap { request ->
                val category = workspace.categories.asSequence()
                    .firstOrNull { category -> category.id == request.category }
                    ?: throw ApiValidationException("Category ${request.category} is not found")

                documentService.getDocumentsByIds(request.attachments)
                    .collectList()
                    .flatMap { attachments ->
                        // TODO validation that all documents are within expense's workspace

                        expenseService.saveExpense(
                            Expense(
                                category = category,
                                timeRecorded = timeService.currentTime(),
                                datePaid = request.datePaid,
                                currency = request.currency,
                                originalAmount = request.originalAmount,
                                amountInDefaultCurrency = request.amountInDefaultCurrency,
                                actualAmountInDefaultCurrency = request.actualAmountInDefaultCurrency,
                                notes = request.notes,
                                percentOnBusiness = request.percentOnBusiness,
                                attachments = attachments,
                                reportedAmountInDefaultCurrency = request.actualAmountInDefaultCurrency
                            )
                        )
                    }
            }
            .map(::mapExpenseDto)
    }

    @GetMapping
    @ApiDto(ExpenseDto::class)
    fun createExpense(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<Expense>> = extensions.withAccessibleWorkspace(workspaceId) { workspace ->
        expenseService.getExpenses(pageRequest.page)
    }
}

data class ExpenseDto(
    var category: Long,
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
    @NotNull var category: Long,
    @NotNull var datePaid: LocalDate,
    @NotBlank var currency: String,
    @NotNull var originalAmount: Long,
    @NotNull var amountInDefaultCurrency: Long,
    @NotNull var actualAmountInDefaultCurrency: Long,
    @NotNull var attachments: List<Long>,
    @NotNull var percentOnBusiness: Int,
    @Size(max = 1024) var notes: String?
)

private fun mapExpenseDto(source: Expense) = ExpenseDto(
    category = source.category.id!!,
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

@Component
class ExpenseDtoMapper
    : ApiDtoMapperAdapter<Expense, ExpenseDto>(Expense::class.java, ExpenseDto::class.java) {

    override fun map(source: Expense) = mapExpenseDto(source)
}