package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.impl

import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQuery
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QIncome
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeRepositoryExt
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomesStatistics
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.QIncomesStatistics
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.persistence.EntityManager

@Component
class IncomeRepositoryExtImpl(
    private val entityManager: EntityManager
) : IncomeRepositoryExt {

    override fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<IncomesStatistics> {

        val income = QIncome.income
        return JPAQuery<IncomesStatistics>(entityManager)
            .from(income)
            .where(
                income.category.workspace.eq(workspace),
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
}