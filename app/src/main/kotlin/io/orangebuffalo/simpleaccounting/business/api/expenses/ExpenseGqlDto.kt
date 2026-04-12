package io.orangebuffalo.simpleaccounting.business.api.expenses

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.categories.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.loadCategoryById
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.documents.loadDocumentsByIds
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@GraphQLName("Expense")
@GraphQLDescription("Business expense.")
data class ExpenseGqlDto(
    @GraphQLDescription("ID of the expense.")
    val id: Long,

    @GraphQLDescription("Version of the expense for optimistic locking.")
    val version: Int,

    @GraphQLDescription("Title of the expense.")
    val title: String,

    @GraphQLDescription("Date when the expense was paid.")
    val datePaid: LocalDate,

    @GraphQLDescription("Currency of the expense.")
    val currency: String,

    @GraphQLDescription("Original amount of the expense in original currency, in cents.")
    val originalAmount: Long,

    @GraphQLDescription("Amounts converted to the default currency.")
    val convertedAmounts: ExpenseAmountsGqlDto,

    @GraphQLDescription("Whether different exchange rate is used for income tax purposes.")
    val useDifferentExchangeRateForIncomeTaxPurposes: Boolean,

    @GraphQLDescription("Amounts for income tax purposes in the default currency.")
    val incomeTaxableAmounts: ExpenseAmountsGqlDto,

    @GraphQLDescription("Percentage of the expense on business.")
    val percentOnBusiness: Int,

    @GraphQLDescription("Optional notes for the expense.")
    val notes: String?,

    @GraphQLDescription("Time when the expense was created, as ISO 8601 timestamp.")
    val createdAt: Instant,

    @GraphQLDescription("Status of the expense.")
    val status: ExpenseStatus,

    @GraphQLDescription("ID of the general tax applied to this expense.")
    val generalTaxId: Long?,

    @GraphQLDescription("Rate of the general tax in basis points.")
    val generalTaxRateInBps: Int?,

    @GraphQLDescription("Amount of the general tax in cents.")
    val generalTaxAmount: Long?,

    @GraphQLIgnore val categoryId: Long?,

    @GraphQLIgnore val attachmentIds: List<Long>,
) {
    @GraphQLDescription("Category of the expense.")
    fun category(env: DataFetchingEnvironment): CompletableFuture<CategoryGqlDto?>? {
        val catId = categoryId ?: return null
        return env.loadCategoryById(catId)
    }

    @GraphQLDescription("Documents attached to this expense.")
    suspend fun attachments(env: DataFetchingEnvironment): List<DocumentGqlDto> {
        if (attachmentIds.isEmpty()) return emptyList()
        return env.loadDocumentsByIds(attachmentIds)
    }
}

@GraphQLName("ExpenseAmounts")
@GraphQLDescription("Amounts for an expense in the default currency.")
data class ExpenseAmountsGqlDto(
    @GraphQLDescription("Original amount in the default currency, before adjustments.")
    val originalAmountInDefaultCurrency: Long?,

    @GraphQLDescription("Adjusted amount in the default currency.")
    val adjustedAmountInDefaultCurrency: Long?,
)
