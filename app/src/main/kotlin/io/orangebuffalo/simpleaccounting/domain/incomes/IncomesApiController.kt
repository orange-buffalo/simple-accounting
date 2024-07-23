package io.orangebuffalo.simpleaccounting.domain.incomes

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.business.*
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
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
@RequestMapping("/api/workspaces/{workspaceId}/incomes")
class IncomesApiController(
    private val incomeService: IncomeService,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @PostMapping
    suspend fun createIncome(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditIncomeDto
    ): IncomeDto = incomeService
        .saveIncome(
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
                status = IncomeStatus.PENDING_CONVERSION,
                linkedInvoiceId = request.linkedInvoice
            )
        )
        .mapToIncomeDto()

    @GetMapping
    suspend fun getIncomes(
        @PathVariable workspaceId: Long,
        @ParameterObject request: IncomesFilteringRequest
    ): ApiPage<IncomeDto> = filteringApiExecutor.executeFiltering(request, workspaceId)

    @GetMapping("{incomeId}")
    suspend fun getIncome(
        @PathVariable workspaceId: Long,
        @PathVariable incomeId: Long
    ): IncomeDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val income = incomeService.getIncomeByIdAndWorkspace(incomeId, workspace)
            ?: throw EntityNotFoundException("Income $incomeId is not found")
        return income.mapToIncomeDto()
    }

    @PutMapping("{incomeId}")
    @Suppress("DuplicatedCode")
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
                linkedInvoiceId = request.linkedInvoice
            }
            .let { incomeService.saveIncome(it) }
            .mapToIncomeDto()
    }

    private val filteringApiExecutor =
        filteringApiExecutorBuilder.executor<Income, IncomeDto, NoOpSorting, IncomesFilteringRequest> {
            query(Tables.INCOME) {
                val category = Tables.CATEGORY
                configure {
                    query.leftJoin(category).on(root.categoryId.eq(category.id))
                }

                onFilter(IncomesFilteringRequest::freeSearchText) { searchText ->
                    or(
                        category.name.containsIgnoreCase(searchText),
                        root.notes.containsIgnoreCase(searchText),
                        root.title.containsIgnoreCase(searchText)
                    )
                }
                addDefaultSorting { root.dateReceived.desc() }
                addDefaultSorting { root.timeRecorded.asc() }
                workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
            }

            mapper { mapToIncomeDto() }
        }
}

class IncomesFilteringRequest : ApiPageRequest<NoOpSorting>() {
    override var sortBy: NoOpSorting? = null

    @field:Parameter(name = "freeSearchText[eq]")
    var freeSearchText: String? = null
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
    val linkedInvoice: Long?,
    val generalTax: Long?,
    val generalTaxRateInBps: Int?,
    val generalTaxAmount: Long?,
    val convertedAmounts: IncomeAmountsDto,
    val incomeTaxableAmounts: IncomeAmountsDto,
    val useDifferentExchangeRateForIncomeTaxPurposes: Boolean
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
    val generalTax: Long?,
    val linkedInvoice: Long?
)

private fun List<Long>?.toIncomeAttachments(): Set<IncomeAttachment> =
    this?.asSequence()?.map(::IncomeAttachment)?.toSet() ?: emptySet()

private fun Income.mapToIncomeDto(): IncomeDto = IncomeDto(
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
    linkedInvoice = linkedInvoiceId,
    generalTax = generalTaxId,
    generalTaxAmount = generalTaxAmount,
    generalTaxRateInBps = generalTaxRateInBps
)

private fun AmountsInDefaultCurrency.mapToAmountsDto() = IncomeAmountsDto(
    originalAmountInDefaultCurrency = originalAmountInDefaultCurrency,
    adjustedAmountInDefaultCurrency = adjustedAmountInDefaultCurrency
)
