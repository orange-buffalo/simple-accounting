package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomeRepository
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomesStatistics
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomeService(
    private val incomeRepository: IncomeRepository,
    private val workspaceService: WorkspaceService,
    private val generalTaxService: GeneralTaxService,
    private val categoryService: CategoryService,
    private val documentsService: DocumentsService
) {
    suspend fun saveIncome(income: Income): Income {
        val workspace = workspaceService.getAccessibleWorkspace(income.workspaceId, WorkspaceAccessMode.READ_WRITE)
        validateIncomeCategory(income)
        validateIncomeAttachments(income)

        val defaultCurrency = workspace.defaultCurrency
        if (defaultCurrency == income.currency) {
            income.convertedAmounts = AmountsInDefaultCurrency(income.originalAmount, null)
            income.incomeTaxableAmounts = AmountsInDefaultCurrency(income.originalAmount, null)
            income.useDifferentExchangeRateForIncomeTaxPurposes = false
        }

        if (!income.useDifferentExchangeRateForIncomeTaxPurposes) {
            income.incomeTaxableAmounts = income.convertedAmounts
        }

        val generalTax = generalTaxService.getValidGeneralTax(income.generalTaxId, workspace)
        income.generalTaxRateInBps = generalTax?.rateInBps

        val convertedAdjustedAmounts = calculateAdjustedAmount(income.convertedAmounts, generalTax)
        income.convertedAmounts.adjustedAmountInDefaultCurrency = convertedAdjustedAmounts.adjustedAmount

        val incomeTaxableAdjustedAmounts = calculateAdjustedAmount(income.incomeTaxableAmounts, generalTax)
        income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency = incomeTaxableAdjustedAmounts.adjustedAmount
        income.generalTaxAmount = incomeTaxableAdjustedAmounts.generalTaxAmount

        income.status = when {
            income.convertedAmounts.adjustedAmountInDefaultCurrency == null -> IncomeStatus.PENDING_CONVERSION
            income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency == null ->
                IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
            else -> IncomeStatus.FINALIZED
        }

        return withDbContext {
            incomeRepository.save(income)
        }
    }

    private suspend fun validateIncomeAttachments(income: Income) {
        if (income.attachments.isEmpty()) {
            return
        }
        val attachmentsIds = income.attachments.map { it.documentId }
        val validAttachmentsIds = documentsService.getValidDocumentsIds(income.workspaceId, attachmentsIds)
        val notValidAttachmentsIds = attachmentsIds.minus(validAttachmentsIds)
        if (notValidAttachmentsIds.isNotEmpty()) {
            throw EntityNotFoundException("Documents $notValidAttachmentsIds are not found")
        }
    }

    private suspend fun validateIncomeCategory(income: Income) {
        if (income.categoryId != null && !categoryService.isValidCategory(income.workspaceId, income.categoryId!!)) {
            throw EntityNotFoundException("Category ${income.categoryId} is not found")
        }
    }

    private fun calculateAdjustedAmount(
        targetAmounts: AmountsInDefaultCurrency,
        generalTax: GeneralTax?
    ): AdjustedAmounts {
        val originalAmountInDefaultCurrency = targetAmounts.originalAmountInDefaultCurrency
            ?: return AdjustedAmounts(null, null)

        if (generalTax == null) {
            return AdjustedAmounts(
                generalTaxAmount = null,
                adjustedAmount = originalAmountInDefaultCurrency
            )
        }

        val baseAmountForAddedGeneralTax = originalAmountInDefaultCurrency.bpsBasePart(generalTax.rateInBps)
        return AdjustedAmounts(
            generalTaxAmount = originalAmountInDefaultCurrency.minus(baseAmountForAddedGeneralTax),
            adjustedAmount = baseAmountForAddedGeneralTax
        )
    }

    suspend fun getIncomeByIdAndWorkspace(incomeId: Long, workspace: Workspace): Income? =
        getIncomeByIdAndWorkspaceId(incomeId, workspace.id!!)

    suspend fun getIncomeByIdAndWorkspaceId(incomeId: Long, workspaceId: Long): Income? =
        withDbContext {
            incomeRepository.findByIdAndWorkspaceId(incomeId, workspaceId)
        }

    suspend fun getIncomesStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<IncomesStatistics> = withDbContext {
        incomeRepository.getStatistics(fromDate, toDate, workspace)
    }

    suspend fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics> = withDbContext {
        incomeRepository.getCurrenciesUsageStatistics(workspace)
    }

    private data class AdjustedAmounts(
        val generalTaxAmount: Long?,
        val adjustedAmount: Long?
    )
}
