package io.orangebuffalo.simpleaccounting.business.incometaxbrackets

import io.orangebuffalo.simpleaccounting.business.countries.CountryFinancialRegistry
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseService
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.MonthDay

enum class IncomeTaxEstimateUnavailableReason {
    PARTIAL_YEAR,
    NO_TAX_RATES,
    CURRENCY_MISMATCH,
}

data class IncomeTaxEstimate(
    val amount: Long? = null,
    val unavailableReason: IncomeTaxEstimateUnavailableReason? = null,
    val taxCurrency: String? = null,
)

@Service
class IncomeTaxEstimationService(
    private val schedulesRepository: IncomeTaxSchedulesRepository,
    private val incomesService: IncomesService,
    private val expenseService: ExpenseService,
) {
    fun estimate(workspace: Workspace, fromDate: LocalDate, toDate: LocalDate): IncomeTaxEstimate {
        val yearStart = CountryFinancialRegistry.byResidency[workspace.residency]?.individualIncomeTaxYearStart
            ?: return IncomeTaxEstimate(unavailableReason = IncomeTaxEstimateUnavailableReason.NO_TAX_RATES)
        if (yearStart != MonthDay.from(fromDate) || toDate != fromDate.plusYears(1).minusDays(1)) {
            return IncomeTaxEstimate(unavailableReason = IncomeTaxEstimateUnavailableReason.PARTIAL_YEAR)
        }

        val schedule = schedulesRepository.findByCountryCodeAndPeriodStartAndPeriodEndExclusive(
            workspace.residency, fromDate, toDate.plusDays(1)
        ).singleOrNull() ?: return IncomeTaxEstimate(unavailableReason = IncomeTaxEstimateUnavailableReason.NO_TAX_RATES)
        if (schedule.currency != workspace.defaultCurrency) {
            return IncomeTaxEstimate(
                unavailableReason = IncomeTaxEstimateUnavailableReason.CURRENCY_MISMATCH,
                taxCurrency = schedule.currency,
            )
        }

        val brackets = schedule.brackets.sortedBy { it.threshold }
        if (brackets.firstOrNull()?.threshold?.compareTo(BigDecimal.ZERO) != 0) {
            return IncomeTaxEstimate(unavailableReason = IncomeTaxEstimateUnavailableReason.NO_TAX_RATES)
        }

        val workspaceId = requireNotNull(workspace.id)
        val taxableAmount = (incomesService.getIncomesStatistics(fromDate, toDate, workspaceId).sumOf { it.totalAmount } -
            expenseService.getExpensesStatistics(fromDate, toDate, workspaceId).sumOf { it.totalAmount })
            .coerceAtLeast(0L)
        val taxableCents = BigDecimal.valueOf(taxableAmount)
        val estimatedCents = brackets.mapIndexed { index, bracket ->
            val lowerCents = bracket.threshold.movePointRight(2)
            val upperCents = brackets.getOrNull(index + 1)?.threshold?.movePointRight(2) ?: taxableCents
            taxableCents.min(upperCents).subtract(lowerCents).max(BigDecimal.ZERO).multiply(bracket.rate)
        }.fold(BigDecimal.ZERO, BigDecimal::add)
        return IncomeTaxEstimate(amount = estimatedCents.setScale(0, RoundingMode.HALF_UP).longValueExact())
    }
}
