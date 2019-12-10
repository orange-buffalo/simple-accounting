package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.querydsl.core.types.dsl.Expressions
import io.orangebuffalo.simpleaccounting.services.business.*
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.toSort
import io.orangebuffalo.simpleaccounting.web.api.integration.EntityNotFoundException
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
                    attachments = extensions.getValidDocuments(workspace, request.attachments),
                    generalTax = extensions.getValidGeneralTax(request.generalTax, workspace),
                    status = IncomeStatus.PENDING_CONVERSION
                )
            ).mapToIncomeDto(invoiceService)
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
        return income.mapToIncomeDto(invoiceService)
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
                convertedAmounts.originalAmountInDefaultCurrency = request.convertedAmountInDefaultCurrency
                incomeTaxableAmounts.originalAmountInDefaultCurrency = request.incomeTaxableAmountInDefaultCurrency
                useDifferentExchangeRateForIncomeTaxPurposes = request.useDifferentExchangeRateForIncomeTaxPurposes
                notes = request.notes
                attachments = extensions.getValidDocuments(workspace, request.attachments)
                generalTax = extensions.getValidGeneralTax(request.generalTax, workspace)
            }
            .let {
                incomeService.saveIncome(it)
            }
            .mapToIncomeDto(invoiceService)
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
    val attachments: List<Long>,
    val notes: String?,
    val id: Long,
    val version: Int,
    val status: IncomeStatus,
    val linkedInvoice: LinkedInvoiceDto?,
    val generalTax: Long?,
    val generalTaxRateInBps: Int?,
    val generalTaxAmount: Long?,
    val convertedAmounts: IncomeAmountsDto,
    val incomeTaxableAmounts: IncomeAmountsDto,
    val useDifferentExchangeRateForIncomeTaxPurposes: Boolean
)

data class LinkedInvoiceDto(
    val id: Long,
    val title: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class IncomeAmountsDto(
    val originalAmountInDefaultCurrency: Long?,
    val adjustedAmountInDefaultCurrency: Long?
)

data class EditIncomeDto(
    val category: Long?,
    val dateReceived: LocalDate,
    @field:NotBlank val title: String,
    @field:NotBlank val currency: String,
    val originalAmount: Long,
    val convertedAmountInDefaultCurrency: Long?,
    val useDifferentExchangeRateForIncomeTaxPurposes: Boolean,
    val incomeTaxableAmountInDefaultCurrency: Long?,
    val attachments: List<Long>?,
    @field:Size(max = 1024) val notes: String?,
    val generalTax: Long?
)

private suspend fun Income.mapToIncomeDto(invoiceService: InvoiceService): IncomeDto {
    val linkedInvoice: Invoice? = invoiceService.findByIncome(this)
    return IncomeDto(
        category = category?.id,
        title = title,
        dateReceived = dateReceived,
        timeRecorded = timeRecorded,
        currency = currency,
        originalAmount = originalAmount,
        convertedAmounts = convertedAmounts.mapToAmountsDto(),
        attachments = attachments.map { it.id!! },
        incomeTaxableAmounts = incomeTaxableAmounts.mapToAmountsDto(),
        useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
        notes = notes,
        id = id!!,
        version = version,
        status = status,
        linkedInvoice = linkedInvoice?.let {
            LinkedInvoiceDto(
                id = it.id!!,
                title = it.title
            )
        },
        generalTax = generalTax?.id,
        generalTaxAmount = generalTaxAmount,
        generalTaxRateInBps = generalTaxRateInBps
    )
}

private fun AmountsInDefaultCurrency.mapToAmountsDto() =
    IncomeAmountsDto(
        originalAmountInDefaultCurrency = originalAmountInDefaultCurrency,
        adjustedAmountInDefaultCurrency = adjustedAmountInDefaultCurrency
    )

@Component
class IncomePageableApiDescriptor(
    private val invoiceService: InvoiceService
) : PageableApiDescriptor<Income, QIncome> {

    override suspend fun mapEntityToDto(entity: Income) = entity.mapToIncomeDto(invoiceService)

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
    }

    override fun getDefaultSorting(): Sort = QIncome.income.dateReceived.desc().toSort()
}
