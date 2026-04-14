package io.orangebuffalo.simpleaccounting.business.api.incomes

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@Component
@Validated
class EditIncomeMutation(
    private val incomesService: IncomesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing income in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editIncome(
        @GraphQLDescription("ID of the workspace the income belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the income to update.")
        id: Long,
        @GraphQLDescription("New title of the income.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("New date when the income was received.")
        dateReceived: LocalDate,
        @GraphQLDescription("New currency of the income.")
        @NotBlank
        currency: String,
        @GraphQLDescription("New original amount of the income in original currency, in cents.")
        originalAmount: Long,
        @GraphQLDescription("New converted amount in the default currency, in cents. Null if not yet converted.")
        convertedAmountInDefaultCurrency: Long?,
        @GraphQLDescription("Whether different exchange rate is used for income tax purposes.")
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean,
        @GraphQLDescription("New amount for income tax purposes in the default currency, in cents. Null if not yet converted.")
        incomeTaxableAmountInDefaultCurrency: Long?,
        @GraphQLDescription("New optional notes for the income.")
        @Size(max = 1024)
        notes: String?,
        @GraphQLDescription("New IDs of documents attached to this income.")
        attachments: List<Long>?,
        @GraphQLDescription("New ID of the category for this income.")
        categoryId: Long?,
        @GraphQLDescription("New ID of the general tax applied to this income.")
        generalTaxId: Long?,
        @GraphQLDescription("New ID of the invoice linked to this income.")
        linkedInvoiceId: Long?,
    ): IncomeGqlDto {
        val income = incomesService.getIncomeByIdAndWorkspaceId(id, workspaceId)
            ?: throw EntityNotFoundException("Income $id is not found")

        income.categoryId = categoryId
        income.title = title
        income.dateReceived = dateReceived
        income.currency = currency
        income.originalAmount = originalAmount
        income.convertedAmounts = AmountsInDefaultCurrency(
            originalAmountInDefaultCurrency = convertedAmountInDefaultCurrency,
            adjustedAmountInDefaultCurrency = null,
        )
        income.incomeTaxableAmounts = AmountsInDefaultCurrency(
            originalAmountInDefaultCurrency = incomeTaxableAmountInDefaultCurrency,
            adjustedAmountInDefaultCurrency = null,
        )
        income.useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes
        income.notes = notes
        income.attachments = mapAttachments(attachments)
        income.generalTaxId = generalTaxId
        income.linkedInvoiceId = linkedInvoiceId

        return incomesService.saveIncome(income).toIncomeGqlDto()
    }

    private fun mapAttachments(attachmentIds: List<Long>?): Set<IncomeAttachment> =
        attachmentIds?.map(::IncomeAttachment)?.toSet() ?: emptySet()
}
