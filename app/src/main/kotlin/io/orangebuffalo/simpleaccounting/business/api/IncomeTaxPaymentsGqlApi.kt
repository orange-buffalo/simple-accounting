package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadDocumentById
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

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
    fun attachments(env: DataFetchingEnvironment): CompletableFuture<List<DocumentGqlDto>> {
        if (attachmentIds.isEmpty()) return CompletableFuture.completedFuture(emptyList())
        val futures = attachmentIds.map { id -> env.loadDocumentById(id) }
        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply { futures.mapNotNull { it.get() } }
    }
}
