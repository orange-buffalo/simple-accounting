package io.orangebuffalo.simpleaccounting.business.api.incomes

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.categories.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.loadCategoryById
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.documents.loadDocumentsByIds
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.GeneralTaxGqlDto
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.loadGeneralTaxByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.invoices.InvoiceGqlDto
import io.orangebuffalo.simpleaccounting.business.api.invoices.loadInvoiceByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@GraphQLName("Income")
@GraphQLDescription("Income record of a workspace.")
data class IncomeGqlDto(
    @GraphQLDescription("ID of the income.")
    val id: Long,

    @GraphQLDescription("Version of the income for optimistic locking.")
    val version: Int,

    @GraphQLDescription("Title of the income.")
    val title: String,

    @GraphQLDescription("Date when the income was received.")
    val dateReceived: LocalDate,

    @GraphQLDescription("Currency of the income.")
    val currency: String,

    @GraphQLDescription("Amount in original currency.")
    val originalAmount: Long,

    @GraphQLDescription("Converted amounts in default currency.")
    val convertedAmounts: IncomeAmountsGqlDto,

    @GraphQLDescription("Indicates if income taxable amounts use a different exchange rate.")
    val useDifferentExchangeRateForIncomeTaxPurposes: Boolean,

    @GraphQLDescription("Amounts for income tax purposes.")
    val incomeTaxableAmounts: IncomeAmountsGqlDto,

    @GraphQLDescription("Optional notes for the income.")
    val notes: String?,

    @GraphQLDescription("Time when the income was created, as ISO 8601 timestamp.")
    val createdAt: Instant,

    @GraphQLDescription("Status of the income.")
    val status: IncomeStatus,

    @GraphQLDescription("Rate in basis points of the general tax applied to this income.")
    val generalTaxRateInBps: Int?,

    @GraphQLDescription("Amount of general tax applied to this income.")
    val generalTaxAmount: Long?,

    @GraphQLIgnore val generalTaxId: Long?,

    @GraphQLIgnore val linkedInvoiceId: Long?,

    @GraphQLIgnore val categoryId: Long?,

    @GraphQLIgnore val workspaceId: Long,

    @GraphQLIgnore val attachmentIds: List<Long>,
) {
    @GraphQLDescription("General tax applied to this income.")
    fun generalTax(env: DataFetchingEnvironment): CompletableFuture<GeneralTaxGqlDto?>? {
        val taxId = generalTaxId ?: return null
        return env.loadGeneralTaxByWorkspaceAndId(workspaceId = workspaceId, taxId = taxId)
    }

    @GraphQLDescription("Category of this income.")
    fun category(env: DataFetchingEnvironment): CompletableFuture<CategoryGqlDto?>? {
        val catId = categoryId ?: return null
        return env.loadCategoryById(catId)
    }

    @GraphQLDescription("Documents attached to this income.")
    suspend fun attachments(env: DataFetchingEnvironment): List<DocumentGqlDto> {
        if (attachmentIds.isEmpty()) return emptyList()
        return env.loadDocumentsByIds(attachmentIds)
    }

    @GraphQLDescription("Invoice linked to this income.")
    fun linkedInvoice(env: DataFetchingEnvironment): CompletableFuture<InvoiceGqlDto?>? {
        val invoiceId = linkedInvoiceId ?: return null
        return env.loadInvoiceByWorkspaceAndId(workspaceId = workspaceId, invoiceId = invoiceId)
    }
}

@GraphQLName("IncomeAmounts")
@GraphQLDescription("Amounts in default currency for an income.")
data class IncomeAmountsGqlDto(
    @GraphQLDescription("Original amount in default currency before tax deduction.")
    val originalAmountInDefaultCurrency: Long?,

    @GraphQLDescription("Adjusted amount in default currency after tax deduction.")
    val adjustedAmountInDefaultCurrency: Long?,
)

fun io.orangebuffalo.simpleaccounting.business.incomes.Income.toIncomeGqlDto() = IncomeGqlDto(
    id = id!!,
    version = version!!,
    title = title,
    dateReceived = dateReceived,
    currency = currency,
    originalAmount = originalAmount,
    convertedAmounts = IncomeAmountsGqlDto(
        originalAmountInDefaultCurrency = convertedAmounts.originalAmountInDefaultCurrency,
        adjustedAmountInDefaultCurrency = convertedAmounts.adjustedAmountInDefaultCurrency,
    ),
    useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
    incomeTaxableAmounts = IncomeAmountsGqlDto(
        originalAmountInDefaultCurrency = incomeTaxableAmounts.originalAmountInDefaultCurrency,
        adjustedAmountInDefaultCurrency = incomeTaxableAmounts.adjustedAmountInDefaultCurrency,
    ),
    notes = notes,
    createdAt = createdAt!!,
    status = status,
    generalTaxRateInBps = generalTaxRateInBps,
    generalTaxAmount = generalTaxAmount,
    generalTaxId = generalTaxId,
    categoryId = categoryId,
    workspaceId = workspaceId,
    attachmentIds = attachments.map { it.documentId },
    linkedInvoiceId = linkedInvoiceId,
)
