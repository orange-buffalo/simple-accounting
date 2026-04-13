package io.orangebuffalo.simpleaccounting.business.api.invoices

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.customers.CustomerGqlDto
import io.orangebuffalo.simpleaccounting.business.api.customers.loadCustomerByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.documents.loadDocumentsByIds
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.GeneralTaxGqlDto
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.loadGeneralTaxByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@GraphQLName("Invoice")
@GraphQLDescription("Invoice for a customer.")
data class InvoiceGqlDto(
    @GraphQLDescription("ID of the invoice.")
    val id: Long,

    @GraphQLDescription("Version of the invoice for optimistic locking.")
    val version: Int,

    @GraphQLDescription("Title of the invoice.")
    val title: String,

    @GraphQLDescription("Date when the invoice was issued.")
    val dateIssued: LocalDate,

    @GraphQLDescription("Date when the invoice was sent.")
    val dateSent: LocalDate?,

    @GraphQLDescription("Date when the invoice was paid.")
    val datePaid: LocalDate?,

    @GraphQLDescription("Time when the invoice was cancelled, as ISO 8601 timestamp.")
    val timeCancelled: Instant?,

    @GraphQLDescription("Due date of the invoice.")
    val dueDate: LocalDate,

    @GraphQLDescription("Currency of the invoice.")
    val currency: String,

    @GraphQLDescription("Amount of the invoice in cents.")
    val amount: Long,

    @GraphQLDescription("Optional notes for the invoice.")
    val notes: String?,

    @GraphQLDescription("Time when the invoice was created, as ISO 8601 timestamp.")
    val createdAt: Instant,

    @GraphQLDescription("Status of the invoice.")
    val status: InvoiceStatus,

    @GraphQLIgnore val generalTaxId: Long?,

    @GraphQLIgnore val customerId: Long,

    @GraphQLIgnore val workspaceId: Long,

    @GraphQLIgnore val attachmentIds: List<Long>,
) {
    @GraphQLDescription("General tax applied to this invoice.")
    fun generalTax(env: DataFetchingEnvironment): CompletableFuture<GeneralTaxGqlDto?>? {
        val taxId = generalTaxId ?: return null
        return env.loadGeneralTaxByWorkspaceAndId(workspaceId = workspaceId, taxId = taxId)
    }

    @GraphQLDescription("Customer of the invoice.")
    fun customer(env: DataFetchingEnvironment): CompletableFuture<CustomerGqlDto?> =
        env.loadCustomerByWorkspaceAndId(workspaceId = workspaceId, customerId = customerId)

    @GraphQLDescription("Documents attached to this invoice.")
    suspend fun attachments(env: DataFetchingEnvironment): List<DocumentGqlDto> {
        if (attachmentIds.isEmpty()) return emptyList()
        return env.loadDocumentsByIds(attachmentIds)
    }
}

fun io.orangebuffalo.simpleaccounting.business.invoices.Invoice.toInvoiceGqlDto(workspaceId: Long) = InvoiceGqlDto(
    id = id!!,
    version = version!!,
    title = title,
    dateIssued = dateIssued,
    dateSent = dateSent,
    datePaid = datePaid,
    timeCancelled = timeCancelled,
    dueDate = dueDate,
    currency = currency,
    amount = amount,
    notes = notes,
    createdAt = createdAt!!,
    status = status,
    generalTaxId = generalTaxId,
    customerId = customerId,
    workspaceId = workspaceId,
    attachmentIds = attachments.map { it.documentId },
)
