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
        workspaceId: String,
        @GraphQLDescription("ID of the income to update.")
        id: String,
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
        convertedAmountInDefaultCurrency: Long? = null,
        @GraphQLDescription("Whether different exchange rate is used for income tax purposes.")
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean,
        @GraphQLDescription("New amount for income tax purposes in the default currency, in cents. Null if not yet converted.")
        incomeTaxableAmountInDefaultCurrency: Long? = null,
        @GraphQLDescription("New optional notes for the income.")
        @Size(max = 1024)
        notes: String? = null,
        @GraphQLDescription("New IDs of documents attached to this income.")
        attachments: List<String>? = null,
        @GraphQLDescription("New ID of the category for this income.")
        categoryId: String? = null,
        @GraphQLDescription("New ID of the general tax applied to this income.")
        generalTaxId: String? = null,
        @GraphQLDescription("New ID of the invoice linked to this income.")
        linkedInvoiceId: String? = null,
    ): IncomeGqlDto {
        val income = incomesService.getIncomeByIdAndWorkspaceId(id, workspaceId)
            ?: throw EntityNotFoundException("Income $id is not found")

        return incomesService.saveIncome(
            income.copy(
                categoryId = categoryId,
                title = title,
                dateReceived = dateReceived,
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
                attachments = mapAttachments(attachments),
                generalTaxId = generalTaxId,
                linkedInvoiceId = linkedInvoiceId,
            )
        ).toIncomeGqlDto()
    }

    private fun mapAttachments(attachmentIds: List<String>?): Set<IncomeAttachment> =
        attachmentIds?.map(::IncomeAttachment)?.toSet() ?: emptySet()
}
