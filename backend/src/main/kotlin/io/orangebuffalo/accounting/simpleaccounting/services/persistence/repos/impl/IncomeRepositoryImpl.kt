package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.impl

import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQuery
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QIncome
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.*
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.persistence.EntityManager

@Component
class IncomeRepositoryExtImpl(
    private val entityManager: EntityManager
) : IncomeRepositoryExt {

    private val income = QIncome.income

    override fun getCurrenciesUsageStatistics(
        workspace: Workspace
    ): List<CurrenciesUsageStatistics> = JPAQuery<CurrenciesUsageStatistics>(entityManager)
        .from(income)
        .groupBy(income.currency)
        .select(
            QCurrenciesUsageStatistics(
                income.currency,
                income.count()
            )
        )
        .fetch()

    override fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<IncomesStatistics> = JPAQuery<IncomesStatistics>(entityManager)
        .from(income)
        .where(
            income.workspace.eq(workspace),
            income.dateReceived.goe(fromDate),
            income.dateReceived.loe(toDate)
        )
        .groupBy(income.category.id)
        .select(
            QIncomesStatistics(
                income.category.id,
                income.reportedAmountInDefaultCurrency.sum(),
                CaseBuilder()
                    .`when`(income.reportedAmountInDefaultCurrency.gt(0)).then(1)
                    .otherwise(Expressions.nullExpression())
                    .count(),
                CaseBuilder()
                    .`when`(income.reportedAmountInDefaultCurrency.eq(0)).then(1)
                    .otherwise(Expressions.nullExpression())
                    .count(),
                CaseBuilder()
                    .`when`(income.reportedAmountInDefaultCurrency.eq(0)).then(0L)
                    .otherwise(income.amountInDefaultCurrency.subtract(income.reportedAmountInDefaultCurrency))
                    .sum()
            )
        )
        .fetch()
}