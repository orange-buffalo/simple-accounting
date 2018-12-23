package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import com.querydsl.core.annotations.QueryProjection
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import java.time.LocalDate

interface ExpenseRepository :
    AbstractEntityRepository<Expense>, QuerydslPredicateExecutor<Expense>, ExpenseRepositoryExt {

    fun findByIdAndCategoryWorkspace(id: Long, workspace: Workspace): Expense?
}

interface ExpenseRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<ExpensesStatistics>
}

data class ExpensesStatistics @QueryProjection constructor(
    val categoryId: Long,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long
)