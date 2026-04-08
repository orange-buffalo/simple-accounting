package io.orangebuffalo.simpleaccounting.business.api.incometaxpayments

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentAttachment
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentService
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@Component
@Validated
class CreateIncomeTaxPaymentMutation(
    private val incomeTaxPaymentService: IncomeTaxPaymentService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new income tax payment in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createIncomeTaxPayment(
        @GraphQLDescription("ID of the workspace to create the income tax payment in.")
        workspaceId: Long,
        @GraphQLDescription("Title of the income tax payment.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("Date when the tax payment was made.")
        datePaid: LocalDate,
        @GraphQLDescription("Date used for reporting purposes. Defaults to datePaid if not specified.")
        reportingDate: LocalDate?,
        @GraphQLDescription("Amount of the tax payment in cents.")
        @Min(1)
        amount: Long,
        @GraphQLDescription("Optional notes for the income tax payment.")
        @Size(max = 1024)
        notes: String?,
        @GraphQLDescription("IDs of documents attached to this income tax payment.")
        attachments: List<Long>?,
    ): IncomeTaxPaymentGqlDto {
        val payment = incomeTaxPaymentService.saveTaxPayment(
            IncomeTaxPayment(
                workspaceId = workspaceId,
                title = title,
                datePaid = datePaid,
                reportingDate = reportingDate ?: datePaid,
                amount = amount,
                notes = notes,
                attachments = mapAttachments(attachments),
            )
        )
        return payment.toIncomeTaxPaymentGqlDto()
    }

    private fun mapAttachments(attachmentIds: List<Long>?): Set<IncomeTaxPaymentAttachment> =
        attachmentIds?.map(::IncomeTaxPaymentAttachment)?.toSet() ?: emptySet()
}
