package io.orangebuffalo.simpleaccounting.business.api.invoices

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceAttachment
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@Component
@Validated
class CreateInvoiceMutation(
    private val invoicesService: InvoicesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new invoice in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createInvoice(
        @GraphQLDescription("ID of the workspace to create the invoice in.")
        workspaceId: Long,
        @GraphQLDescription("ID of the customer for this invoice.")
        customerId: Long,
        @GraphQLDescription("Title of the invoice.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("Date when the invoice was issued.")
        dateIssued: LocalDate,
        @GraphQLDescription("Date when the invoice was sent.")
        dateSent: LocalDate?,
        @GraphQLDescription("Date when the invoice was paid.")
        datePaid: LocalDate?,
        @GraphQLDescription("Due date of the invoice.")
        dueDate: LocalDate,
        @GraphQLDescription("Currency of the invoice.")
        @NotBlank
        currency: String,
        @GraphQLDescription("Amount of the invoice in cents.")
        amount: Long,
        @GraphQLDescription("Optional notes for the invoice.")
        @Size(max = 1024)
        notes: String?,
        @GraphQLDescription("IDs of documents attached to this invoice.")
        attachments: List<Long>?,
        @GraphQLDescription("ID of the general tax applied to this invoice.")
        generalTaxId: Long?,
    ): InvoiceGqlDto {
        val invoice = invoicesService.saveInvoice(
            Invoice(
                customerId = customerId,
                title = title,
                dateIssued = dateIssued,
                dateSent = dateSent,
                datePaid = datePaid,
                dueDate = dueDate,
                currency = currency,
                amount = amount,
                notes = notes,
                attachments = mapAttachments(attachments),
                generalTaxId = generalTaxId,
            ),
            workspaceId,
        )
        return invoice.toInvoiceGqlDto(workspaceId)
    }

    private fun mapAttachments(attachmentIds: List<Long>?): Set<InvoiceAttachment> =
        attachmentIds?.map(::InvoiceAttachment)?.toSet() ?: emptySet()
}
