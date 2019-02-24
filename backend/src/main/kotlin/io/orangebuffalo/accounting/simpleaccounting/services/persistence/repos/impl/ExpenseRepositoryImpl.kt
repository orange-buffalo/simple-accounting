package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.impl

import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQuery
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QExpense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.*
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.persistence.EntityManager

@Component
class ExpenseRepositoryExtImpl(
    private val entityManager: EntityManager
) : ExpenseRepositoryExt {

    private val expense = QExpense.expense

    override fun getCurrenciesUsageStatistics(
        workspace: Workspace
    ): List<CurrenciesUsageStatistics> = JPAQuery<CurrenciesUsageStatistics>(entityManager)
        .from(expense)
        .groupBy(expense.currency)
        .select(
            QCurrenciesUsageStatistics(
                expense.currency,
                expense.count()
            )
        )
        .fetch()

    override fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<ExpensesStatistics> = JPAQuery<ExpensesStatistics>(entityManager)
        .from(expense)
        .where(
            expense.workspace.eq(workspace),
            expense.datePaid.goe(fromDate),
            expense.datePaid.loe(toDate)
        )
        .groupBy(expense.category.id)
        .select(
            QExpensesStatistics(
                expense.category.id,
                expense.reportedAmountInDefaultCurrency.sum(),
                CaseBuilder()
                    .`when`(expense.reportedAmountInDefaultCurrency.gt(0)).then(1)
                    .otherwise(Expressions.nullExpression())
                    .count(),
                CaseBuilder()
                    .`when`(expense.reportedAmountInDefaultCurrency.eq(0)).then(1)
                    .otherwise(Expressions.nullExpression())
                    .count()
            )
        )
        .fetch()
}