package io.orangebuffalo.simpleaccounting.business.api.incometaxpayments

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentAttachment
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentService
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@Component
@Validated
class EditIncomeTaxPaymentMutation(
    private val incomeTaxPaymentService: IncomeTaxPaymentService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing income tax payment in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editIncomeTaxPayment(
        @GraphQLDescription("ID of the workspace the income tax payment belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the income tax payment to update.")
        id: Long,
        @GraphQLDescription("New title of the income tax payment.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("New date when the tax payment was made.")
        @NotNull
        datePaid: LocalDate? = null,
        @GraphQLDescription("New date used for reporting purposes. Defaults to datePaid if not specified.")
        reportingDate: LocalDate?,
        @GraphQLDescription("New amount of the tax payment in cents.")
        @Min(1)
        amount: Long,
        @GraphQLDescription("New optional notes for the income tax payment.")
        @Size(max = 1024)
        notes: String?,
        @GraphQLDescription("New IDs of documents attached to this income tax payment.")
        attachments: List<Long>?,
    ): IncomeTaxPaymentGqlDto {
        val payment = incomeTaxPaymentService.getTaxPaymentByIdAndWorkspace(id, workspaceId)
            ?: throw EntityNotFoundException("IncomeTaxPayment $id is not found")

        payment.title = title
        payment.datePaid = datePaid!!
        payment.reportingDate = reportingDate ?: datePaid!!
        payment.amount = amount
        payment.notes = notes
        payment.attachments = mapAttachments(attachments)

        return incomeTaxPaymentService.saveTaxPayment(payment).toIncomeTaxPaymentGqlDto()
    }

    private fun mapAttachments(attachmentIds: List<Long>?): Set<IncomeTaxPaymentAttachment> =
        attachmentIds?.map(::IncomeTaxPaymentAttachment)?.toSet() ?: emptySet()
}
