package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadDocumentsByIds
import java.time.LocalDate

@GraphQLName("IncomeTaxPayment")
@GraphQLDescription("An income tax payment in a workspace.")
data class IncomeTaxPaymentGqlDto(
    @GraphQLDescription("ID of the income tax payment.")
    val id: Long,

    @GraphQLDescription("Title of the income tax payment.")
    val title: String,

    @GraphQLDescription("Date when the tax payment was made.")
    val datePaid: LocalDate,

    @GraphQLDescription("Date used for reporting purposes.")
    val reportingDate: LocalDate,

    @GraphQLDescription("Amount of the tax payment in cents.")
    val amount: Long,

    @GraphQLDescription("Optional notes for the income tax payment.")
    val notes: String?,

    @GraphQLIgnore val attachmentIds: List<Long>,
) {
    @GraphQLDescription("Documents attached to this income tax payment.")
    suspend fun attachments(env: DataFetchingEnvironment): List<DocumentGqlDto> {
        if (attachmentIds.isEmpty()) return emptyList()
        return env.loadDocumentsByIds(attachmentIds)
    }
}
