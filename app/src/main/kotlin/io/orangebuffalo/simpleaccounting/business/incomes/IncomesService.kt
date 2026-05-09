package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.business.categories.CategoriesService
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesService
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.bpsBasePart
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.infra.executeInParallel
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomesService(
    private val incomeRepository: IncomesRepository,
    private val workspacesService: WorkspacesService,
    private val generalTaxesService: GeneralTaxesService,
    private val categoriesService: CategoriesService,
    private val documentsService: DocumentsService,
    private val invoicesService: InvoicesService
) {
    suspend fun saveIncome(income: Income): Income {
        val workspace = workspacesService.getAccessibleWorkspace(income.workspaceId, WorkspaceAccessMode.READ_WRITE)
        validateCategoryAndAttachments(income)
        updateInvoiceIfLinked(income)

        val isDefaultCurrency = workspace.defaultCurrency == income.currency
        val convertedAmounts = if (isDefaultCurrency) {
            AmountsInDefaultCurrency(income.originalAmount, null)
        } else income.convertedAmounts
        val useDifferentExchangeRateForIncomeTaxPurposes =
            !isDefaultCurrency && income.useDifferentExchangeRateForIncomeTaxPurposes
        val incomeTaxableAmounts = if (useDifferentExchangeRateForIncomeTaxPurposes) {
            income.incomeTaxableAmounts
        } else convertedAmounts

        val generalTax = getGeneralTax(income)
        val convertedAdjustedAmounts = calculateAdjustedAmount(convertedAmounts, generalTax)
        val adjustedConvertedAmounts = convertedAmounts.copy(
            adjustedAmountInDefaultCurrency = convertedAdjustedAmounts.adjustedAmount
        )

        val incomeTaxableAdjustedAmounts = calculateAdjustedAmount(incomeTaxableAmounts, generalTax)
        val adjustedIncomeTaxableAmounts = incomeTaxableAmounts.copy(
            adjustedAmountInDefaultCurrency = incomeTaxableAdjustedAmounts.adjustedAmount
        )
        val status = when {
            adjustedConvertedAmounts.adjustedAmountInDefaultCurrency == null -> IncomeStatus.PENDING_CONVERSION
            adjustedIncomeTaxableAmounts.adjustedAmountInDefaultCurrency == null ->
                IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
            else -> IncomeStatus.FINALIZED
        }

        return withDbContext {
            incomeRepository.save(
                income.copy(
                    convertedAmounts = adjustedConvertedAmounts,
                    incomeTaxableAmounts = adjustedIncomeTaxableAmounts,
                    useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
                    generalTaxRateInBps = generalTax?.rateInBps,
                    generalTaxAmount = incomeTaxableAdjustedAmounts.generalTaxAmount,
                    status = status,
                )
            )
        }
    }

    private suspend fun updateInvoiceIfLinked(income: Income) {
        val invoiceId = income.linkedInvoiceId ?: return

        val invoice = invoicesService.getInvoiceByIdAndWorkspaceId(id = invoiceId, workspaceId = income.workspaceId)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")

        invoicesService.saveInvoice(invoice.copy(datePaid = income.dateReceived), income.workspaceId)
    }

    private suspend fun getGeneralTax(income: Income): GeneralTax? =
        if (income.generalTaxId == null) null else generalTaxesService.getValidGeneralTax(
            income.generalTaxId!!,
            income.workspaceId
        )

    private suspend fun validateCategoryAndAttachments(income: Income) = executeInParallel {
        step { validateIncomeCategory(income) }
        step { validateIncomeAttachments(income) }
    }

    private suspend fun validateIncomeAttachments(income: Income) {
        if (income.attachments.isNotEmpty()) {
            val attachmentsIds = income.attachments.map { it.documentId }
            documentsService.validateDocuments(income.workspaceId, attachmentsIds)
        }
    }

    private suspend fun validateIncomeCategory(income: Income) {
        if (income.categoryId != null) categoriesService.validateCategory(income.categoryId!!, income.workspaceId)
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

    suspend fun getIncomeByIdAndWorkspace(incomeId: String, workspace: Workspace): Income? =
        getIncomeByIdAndWorkspaceId(incomeId, workspace.id!!)

    suspend fun getIncomeByIdAndWorkspaceId(incomeId: String, workspaceId: String): Income? =
        withDbContext {
            incomeRepository.findByIdAndWorkspaceId(incomeId, workspaceId)
        }

    suspend fun getIncomesStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: String
    ): List<IncomesStatistics> = withDbContext {
        incomeRepository.getStatistics(fromDate, toDate, workspaceId)
    }

    suspend fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics> = withDbContext {
        incomeRepository.getCurrenciesUsageStatistics(workspace)
    }

    private data class AdjustedAmounts(
        val generalTaxAmount: Long?,
        val adjustedAmount: Long?
    )
}
