package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.registerEntitySaveListener
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomesStatistics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory

@Service
class IncomeService(
    private val incomeRepository: IncomeRepository,
    private val entityManagerFactory: EntityManagerFactory
) {

    @PostConstruct
    private fun initPersistenceListeners() {
        entityManagerFactory.registerEntitySaveListener(::validateIncomeConsistency)
    }

    /**
     * Sanity check that an [Income] is consistent (i.e. all the denormalized fields
     * are compatible and plausible). Only most critical verifications are provided.
     */
    fun validateIncomeConsistency(income: Income) {
        val isDefaultCurrency = income.currency == income.workspace.defaultCurrency
        if (isDefaultCurrency) {
            require(income.originalAmount == income.convertedAmounts.originalAmountInDefaultCurrency) {
                "Inconsistent income: converted amount does not match original for default currency"
            }

            require(income.originalAmount == income.incomeTaxableAmounts.originalAmountInDefaultCurrency) {
                "Inconsistent income: income taxable amount does not match original for default currency"
            }
        }

        if (!income.useDifferentExchangeRateForIncomeTaxPurposes) {
            require(income.convertedAmounts == income.incomeTaxableAmounts) {
                "Inconsistent income: amounts do not match but same exchange rate is used"
            }
        }

        if (income.status == IncomeStatus.FINALIZED) {
            require(income.convertedAmounts.notEmpty && income.incomeTaxableAmounts.notEmpty) {
                "Inconsistent income: amounts are not provided for finalized income"
            }
        }
    }

    suspend fun saveIncome(income: Income): Income {
        val defaultCurrency = income.workspace.defaultCurrency
        if (defaultCurrency == income.currency) {
            income.convertedAmounts = AmountsInDefaultCurrency(income.originalAmount, null)
            income.incomeTaxableAmounts = AmountsInDefaultCurrency(income.originalAmount, null)
            income.useDifferentExchangeRateForIncomeTaxPurposes = false
        }

        if (!income.useDifferentExchangeRateForIncomeTaxPurposes) {
            income.incomeTaxableAmounts = income.convertedAmounts
        }

        val generalTax = income.generalTax
        income.generalTaxRateInBps = generalTax?.rateInBps

        val convertedAdjustedAmounts = calculateAdjustedAmount(income, income.convertedAmounts)
        income.convertedAmounts.adjustedAmountInDefaultCurrency = convertedAdjustedAmounts.adjustedAmount

        val incomeTaxableAdjustedAmounts = calculateAdjustedAmount(income, income.incomeTaxableAmounts)
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

    private fun calculateAdjustedAmount(income: Income, targetAmounts: AmountsInDefaultCurrency): AdjustedAmounts {
        val originalAmountInDefaultCurrency = targetAmounts.originalAmountInDefaultCurrency
            ?: return AdjustedAmounts(null, null)

        val generalTax = income.generalTax
            ?: return AdjustedAmounts(
                generalTaxAmount = null,
                adjustedAmount = originalAmountInDefaultCurrency
            )

        val baseAmountForAddedGeneralTax = originalAmountInDefaultCurrency.bpsBasePart(generalTax.rateInBps)
        return AdjustedAmounts(
            generalTaxAmount = originalAmountInDefaultCurrency.minus(baseAmountForAddedGeneralTax),
            adjustedAmount = baseAmountForAddedGeneralTax
        )
    }

    suspend fun getIncomes(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Income> = withDbContext {
        incomeRepository.findAll(QIncome.income.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getIncomeByIdAndWorkspace(id: Long, workspace: Workspace): Income? =
        withDbContext {
            incomeRepository.findByIdAndWorkspace(id, workspace)
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
