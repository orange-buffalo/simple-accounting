package io.orangebuffalo.simpleaccounting.business.api.incomes

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@Component
@Validated
class CreateIncomeMutation(
    private val incomesService: IncomesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new income in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createIncome(
        @GraphQLDescription("ID of the workspace to create the income in.")
        workspaceId: Long,
        @GraphQLDescription("Title of the income.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("Date when the income was received.")
        dateReceived: LocalDate,
        @GraphQLDescription("Currency of the income.")
        @NotBlank
        currency: String,
        @GraphQLDescription("Original amount of the income in original currency, in cents.")
        originalAmount: Long,
        @GraphQLDescription("Converted amount in the default currency, in cents. Null if not yet converted.")
        convertedAmountInDefaultCurrency: Long? = null,
        @GraphQLDescription("Whether different exchange rate is used for income tax purposes.")
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean,
        @GraphQLDescription("Amount for income tax purposes in the default currency, in cents. Null if not yet converted.")
        incomeTaxableAmountInDefaultCurrency: Long? = null,
        @GraphQLDescription("Optional notes for the income.")
        @Size(max = 1024)
        notes: String? = null,
        @GraphQLDescription("IDs of documents attached to this income.")
        attachments: List<Long>? = null,
        @GraphQLDescription("ID of the category for this income.")
        categoryId: Long? = null,
        @GraphQLDescription("ID of the general tax applied to this income.")
        generalTaxId: Long? = null,
        @GraphQLDescription("ID of the invoice linked to this income.")
        linkedInvoiceId: Long? = null,
    ): IncomeGqlDto {
        val income = incomesService.saveIncome(
            Income(
                workspaceId = workspaceId,
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
                status = IncomeStatus.PENDING_CONVERSION,
                linkedInvoiceId = linkedInvoiceId,
            )
        )
        return income.toIncomeGqlDto()
    }

    private fun mapAttachments(attachmentIds: List<Long>?): Set<IncomeAttachment> =
        attachmentIds?.map(::IncomeAttachment)?.toSet() ?: emptySet()
}
