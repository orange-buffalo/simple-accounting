package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.ExpenseService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/v1/user/workspaces/{workspaceId}/expenses")
class ExpenseApiController(
    private val extensions: ApiControllersExtensions,
    private val expenseService: ExpenseService
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

                expenseService.saveExpense(
                    Expense(
                        category = category,
                        //todo proper time handling
                        dateRecorded = ZonedDateTime.now(),
                        datePaid = ZonedDateTime.now(),
                        currency = request.currency,
                        originalAmount = request.originalAmount,
                        amountInDefaultCurrency = request.amountInDefaultCurrency,
                        actualAmountInDefaultCurrency = request.actualAmountInDefaultCurrency,
                        notes = request.notes,
                        percentOnBusinessInBps = request.percentOnBusinessInBps,
                        attachments = ArrayList()
                    )
                )
            }
            .map(::mapExpenseDto)
    }
}

data class ExpenseDto(
    var category: Long,
    // todo
//    var dateRecorded: ZonedDateTime,
//    var datePaid: ZonedDateTime,
    var currency: String,
    var originalAmount: Long,
    var amountInDefaultCurrency: Long,
    var actualAmountInDefaultCurrency: Long,
    var attachments: List<Long>,
    var percentOnBusinessInBps: Int,
    var notes: String?,
    var id: Long,
    var version: Int
)

data class CreateExpenseDto(
    @NotNull var category: Long,
    //todo
    //var datePaid: ZonedDateTime,
    @NotBlank var currency: String,
    @NotNull var originalAmount: Long,
    @NotNull var amountInDefaultCurrency: Long,
    @NotNull var actualAmountInDefaultCurrency: Long,
    @NotNull var attachments: List<Long>,
    @NotNull var percentOnBusinessInBps: Int,
    @Size(max = 1024) var notes: String?
)

fun mapExpenseDto(source: Expense) = ExpenseDto(
    category = source.category.id!!,
    // todo
//    datePaid = source.datePaid,
//    dateRecorded = source.dateRecorded,
    currency = source.currency,
    originalAmount = source.originalAmount,
    amountInDefaultCurrency = source.amountInDefaultCurrency,
    actualAmountInDefaultCurrency = source.actualAmountInDefaultCurrency,
    attachments = source.attachments.map { it.id!! },
    percentOnBusinessInBps = source.percentOnBusinessInBps,
    notes = source.notes,
    id = source.id!!,
    version = source.version
)