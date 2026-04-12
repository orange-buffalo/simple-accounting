package io.orangebuffalo.simpleaccounting.business.api.expenses

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseAttachment
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@Component
@Validated
class EditExpenseMutation(
    private val expenseService: ExpenseService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing expense in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editExpense(
        @GraphQLDescription("ID of the workspace the expense belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the expense to update.")
        id: Long,
        @GraphQLDescription("New title of the expense.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("New date when the expense was paid.")
        datePaid: LocalDate,
        @GraphQLDescription("New currency of the expense.")
        @NotBlank
        currency: String,
        @GraphQLDescription("New original amount of the expense in original currency, in cents.")
        originalAmount: Long,
        @GraphQLDescription("New converted amount in the default currency, in cents. Null if not yet converted.")
        convertedAmountInDefaultCurrency: Long?,
        @GraphQLDescription("Whether different exchange rate is used for income tax purposes.")
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean,
        @GraphQLDescription("New amount for income tax purposes in the default currency, in cents. Null if not yet converted.")
        incomeTaxableAmountInDefaultCurrency: Long?,
        @GraphQLDescription("New optional notes for the expense.")
        @Size(max = 1024)
        notes: String?,
        @GraphQLDescription("New percentage of the expense on business. Defaults to 100.")
        @Min(1)
        @Max(100)
        percentOnBusiness: Int?,
        @GraphQLDescription("New IDs of documents attached to this expense.")
        attachments: List<Long>?,
        @GraphQLDescription("New ID of the category for this expense.")
        categoryId: Long?,
        @GraphQLDescription("New ID of the general tax applied to this expense.")
        generalTaxId: Long?,
    ): ExpenseGqlDto {
        val expense = expenseService.getExpenseByIdAndWorkspace(id, workspaceId)
            ?: throw EntityNotFoundException("Expense $id is not found")

        expense.categoryId = categoryId
        expense.title = title
        expense.datePaid = datePaid
        expense.currency = currency
        expense.originalAmount = originalAmount
        expense.convertedAmounts = AmountsInDefaultCurrency(
            originalAmountInDefaultCurrency = convertedAmountInDefaultCurrency,
            adjustedAmountInDefaultCurrency = null,
        )
        expense.incomeTaxableAmounts = AmountsInDefaultCurrency(
            originalAmountInDefaultCurrency = incomeTaxableAmountInDefaultCurrency,
            adjustedAmountInDefaultCurrency = null,
        )
        expense.useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes
        expense.notes = notes
        expense.percentOnBusiness = percentOnBusiness ?: 100
        expense.attachments = mapAttachments(attachments)
        expense.generalTaxId = generalTaxId

        return expenseService.saveExpense(expense).toExpenseGqlDto()
    }

    private fun mapAttachments(attachmentIds: List<Long>?): Set<ExpenseAttachment> =
        attachmentIds?.map(::ExpenseAttachment)?.toSet() ?: emptySet()
}
