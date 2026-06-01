package io.orangebuffalo.simpleaccounting.business.api.documents

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("DocumentUsageFilterType")
@GraphQLDescription("Usage type filter for documents.")
enum class DocumentUsageFilterType {
    @GraphQLDescription("Document is used by an expense.")
    EXPENSE,

    @GraphQLDescription("Document is used by an income.")
    INCOME,

    @GraphQLDescription("Document is used by an invoice.")
    INVOICE,

    @GraphQLDescription("Document is used by an income tax payment.")
    INCOME_TAX_PAYMENT,

    @GraphQLDescription("Document is used by a standalone document.")
    STANDALONE_DOCUMENT,

    @GraphQLDescription("Document is not used by any entity.")
    UNUSED,
}
