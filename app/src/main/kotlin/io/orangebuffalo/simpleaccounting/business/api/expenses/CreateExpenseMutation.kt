package io.orangebuffalo.simpleaccounting.business.api.expenses

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseAttachment
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseService
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@Component
@Validated
class CreateExpenseMutation(
    private val expenseService: ExpenseService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new expense in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createExpense(
        @GraphQLDescription("ID of the workspace to create the expense in.")
        workspaceId: Long,
        @GraphQLDescription("Title of the expense.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("Date when the expense was paid.")
        datePaid: LocalDate,
        @GraphQLDescription("Currency of the expense.")
        @NotBlank
        currency: String,
        @GraphQLDescription("Original amount of the expense in original currency, in cents.")
        originalAmount: Long,
        @GraphQLDescription("Converted amount in the default currency, in cents. Null if not yet converted.")
        convertedAmountInDefaultCurrency: Long?,
        @GraphQLDescription("Whether different exchange rate is used for income tax purposes.")
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean,
        @GraphQLDescription("Amount for income tax purposes in the default currency, in cents. Null if not yet converted.")
        incomeTaxableAmountInDefaultCurrency: Long?,
        @GraphQLDescription("Optional notes for the expense.")
        @Size(max = 1024)
        notes: String?,
        @GraphQLDescription("Percentage of the expense on business. Defaults to 100.")
        percentOnBusiness: Int?,
        @GraphQLDescription("IDs of documents attached to this expense.")
        attachments: List<Long>?,
        @GraphQLDescription("ID of the category for this expense.")
        categoryId: Long?,
        @GraphQLDescription("ID of the general tax applied to this expense.")
        generalTaxId: Long?,
    ): ExpenseGqlDto {
        val expense = expenseService.saveExpense(
            Expense(
                workspaceId = workspaceId,
                categoryId = categoryId,
                title = title,
                datePaid = datePaid,
                currency = currency,
                originalAmount = originalAmount,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = convertedAmountInDefaultCurrency,
                    adjustedAmountInDefaultCurrency = null,
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = incomeTaxableAmountInDefaultCurrency,
                    adjustedAmountInDefaultCurrency = null,
                ),
                useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
                notes = notes,
                percentOnBusiness = percentOnBusiness ?: 100,
                attachments = mapAttachments(attachments),
                generalTaxId = generalTaxId,
                status = ExpenseStatus.PENDING_CONVERSION,
            )
        )
        return expense.toExpenseGqlDto()
    }

    private fun mapAttachments(attachmentIds: List<Long>?): Set<ExpenseAttachment> =
        attachmentIds?.map(::ExpenseAttachment)?.toSet() ?: emptySet()
}
