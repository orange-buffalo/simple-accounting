package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.*
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiPredicateOperator
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/incomes")
class IncomesApiController(
    private val incomeService: IncomeService,
    private val timeService: TimeService,
    private val invoiceService: InvoiceService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @PostMapping
    suspend fun createIncome(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditIncomeDto
    ): IncomeDto = incomeService.saveIncome(
        Income(
            workspaceId = workspaceId,
            categoryId = request.category,
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
            attachments = request.attachments.toIncomeAttachments(),
            generalTaxId = request.generalTax,
            status = IncomeStatus.PENDING_CONVERSION
        )
    ).mapToIncomeDto(invoiceService)

    @GetMapping
    suspend fun getIncomes(@PathVariable workspaceId: Long): ApiPage<IncomeDto> =
        filteringApiExecutor.executeFiltering(workspaceId)

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

        // todo #71: optimistic locking. etag?
        val income = incomeService.getIncomeByIdAndWorkspaceId(incomeId, workspaceId)
            ?: throw EntityNotFoundException("Income $incomeId is not found")

        return income
            .apply {
                categoryId = request.category
                title = request.title
                dateReceived = request.dateReceived
                currency = request.currency
                originalAmount = request.originalAmount
                convertedAmounts.originalAmountInDefaultCurrency = request.convertedAmountInDefaultCurrency
                incomeTaxableAmounts.originalAmountInDefaultCurrency = request.incomeTaxableAmountInDefaultCurrency
                useDifferentExchangeRateForIncomeTaxPurposes = request.useDifferentExchangeRateForIncomeTaxPurposes
                notes = request.notes
                attachments = request.attachments.toIncomeAttachments()
                generalTaxId = request.generalTax
            }
            .let {
                incomeService.saveIncome(it)
            }
            .mapToIncomeDto(invoiceService)
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder.executor<Income, IncomeDto> {
        query(Tables.INCOME) {
            filterByField("freeSearchText", String::class) {
                val category = Tables.CATEGORY
                query.leftJoin(category).on(root.categoryId.eq(category.id))

                onPredicate(FilteringApiPredicateOperator.EQ) { searchText ->
                    val token = searchText.toLowerCase()
                    or(
                        category.name.containsIgnoreCase(token),
                        root.notes.containsIgnoreCase(token),
                        root.title.containsIgnoreCase(token)
                    )
                }
            }
            addDefaultSorting { root.dateReceived.desc() }
            addDefaultSorting { root.timeRecorded.asc() }
            workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
        }

        mapper { mapToIncomeDto(invoiceService) }
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

private fun List<Long>?.toIncomeAttachments(): Set<IncomeAttachment> =
    this?.asSequence()?.map(::IncomeAttachment)?.toSet() ?: emptySet()

private suspend fun Income.mapToIncomeDto(invoiceService: InvoiceService): IncomeDto {
    val linkedInvoice: Invoice? = invoiceService.findByIncome(this)
    return IncomeDto(
        category = categoryId,
        title = title,
        dateReceived = dateReceived,
        timeRecorded = timeRecorded,
        currency = currency,
        originalAmount = originalAmount,
        convertedAmounts = convertedAmounts.mapToAmountsDto(),
        attachments = attachments.map { it.documentId },
        incomeTaxableAmounts = incomeTaxableAmounts.mapToAmountsDto(),
        useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
        notes = notes,
        id = id!!,
        version = version!!,
        status = status,
        linkedInvoice = linkedInvoice?.let {
            LinkedInvoiceDto(
                id = it.id!!,
                title = it.title
            )
        },
        generalTax = generalTaxId,
        generalTaxAmount = generalTaxAmount,
        generalTaxRateInBps = generalTaxRateInBps
    )
}

private fun AmountsInDefaultCurrency.mapToAmountsDto() =
    IncomeAmountsDto(
        originalAmountInDefaultCurrency = originalAmountInDefaultCurrency,
        adjustedAmountInDefaultCurrency = adjustedAmountInDefaultCurrency
    )

