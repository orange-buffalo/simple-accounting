package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeStatus
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.fetchListOf
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomeRepositoryExt
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomesStatistics
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class IncomeRepositoryExtImpl(
    private val dslContext: DSLContext
) : IncomeRepositoryExt {

    private val income = Tables.INCOME

    override fun getCurrenciesUsageStatistics(
        workspace: Workspace
    ): List<CurrenciesUsageStatistics> = dslContext
        .select(income.currency, count(income.id))
        .from(income)
        .groupBy(income.currency)
        .fetchListOf()

    override fun findByIdAndWorkspaceId(incomeId: Long, workspaceId: Long): Income? = dslContext
        .select().from(income)
        .where(
            income.id.eq(incomeId),
            income.workspaceId.eq(workspaceId)
        )
        .fetchOneOrNull()

    override fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<IncomesStatistics> {
        val incomeTaxableAmount = income.incomeTaxableAdjustedAmountInDefaultCurrency
        return dslContext
            .select(
                income.categoryId.`as`("category_Id"),
                //todo #222 extension for fluent sum
                sum(
                    case_()
                        .`when`(incomeTaxableAmount.isNull, 0L)
                        .otherwise(incomeTaxableAmount)
                ).`as`("total_Amount"),
                count(
                    case_()
                        //todo #222 configure jooq to map enum
                        .`when`(income.status.eq(IncomeStatus.FINALIZED.name), 1L)
                        .otherwise(inline<Long>(null))
                //todo #222 cleaner way for pojo mapping
                ).`as`("finalized_Count"),
                count(
                    case_()
                        .`when`(income.status.ne(IncomeStatus.FINALIZED.name), 1L)
                        .otherwise(inline<Long>(null))
                ).`as`("pending_Count"),
                sum(
                    case_()
                        .`when`(income.status.ne(IncomeStatus.FINALIZED.name), 0L)
                        .otherwise(
                            income.convertedAdjustedAmountInDefaultCurrency
                                .sub(income.incomeTaxableAdjustedAmountInDefaultCurrency)
                        )
                ).`as`("currency_Exchange_Difference")
            )
            .from(income)
            .where(
                income.workspaceId.eq(workspace.id),
                income.dateReceived.greaterOrEqual(fromDate),
                income.dateReceived.lessOrEqual(toDate)
            )
            .groupBy(income.categoryId)
            .fetchListOf()
    }
}
