package io.orangebuffalo.simpleaccounting.business.api.invoices

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceAttachment
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@Component
@Validated
class EditInvoiceMutation(
    private val invoicesService: InvoicesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing invoice in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editInvoice(
        @GraphQLDescription("ID of the workspace the invoice belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the invoice to update.")
        id: Long,
        @GraphQLDescription("ID of the customer for this invoice.")
        customerId: Long,
        @GraphQLDescription("New title of the invoice.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("New date when the invoice was issued.")
        dateIssued: LocalDate,
        @GraphQLDescription("New date when the invoice was sent.")
        dateSent: LocalDate? = null,
        @GraphQLDescription("New date when the invoice was paid.")
        datePaid: LocalDate? = null,
        @GraphQLDescription("New due date of the invoice.")
        dueDate: LocalDate,
        @GraphQLDescription("New currency of the invoice.")
        @NotBlank
        currency: String,
        @GraphQLDescription("New amount of the invoice in cents.")
        amount: Long,
        @GraphQLDescription("New optional notes for the invoice.")
        @Size(max = 1024)
        notes: String? = null,
        @GraphQLDescription("New IDs of documents attached to this invoice.")
        attachments: List<Long>? = null,
        @GraphQLDescription("New ID of the general tax applied to this invoice.")
        generalTaxId: Long? = null,
    ): InvoiceGqlDto {
        val invoice = invoicesService.getInvoiceByIdAndWorkspaceId(id, workspaceId)
            ?: throw EntityNotFoundException("Invoice $id is not found")

        invoice.customerId = customerId
        invoice.title = title
        invoice.dateIssued = dateIssued
        invoice.dateSent = dateSent
        invoice.datePaid = datePaid
        invoice.dueDate = dueDate
        invoice.currency = currency
        invoice.amount = amount
        invoice.notes = notes
        invoice.attachments = mapAttachments(attachments)
        invoice.generalTaxId = generalTaxId

        return invoicesService.saveInvoice(invoice, workspaceId).toInvoiceGqlDto(workspaceId)
    }

    private fun mapAttachments(attachmentIds: List<Long>?): Set<InvoiceAttachment> =
        attachmentIds?.map(::InvoiceAttachment)?.toSet() ?: emptySet()
}
