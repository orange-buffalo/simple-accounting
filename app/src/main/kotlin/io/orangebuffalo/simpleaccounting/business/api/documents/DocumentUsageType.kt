package io.orangebuffalo.simpleaccounting.business.api.documents

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("DocumentUsageType")
@GraphQLDescription("Type of entity that uses a document.")
enum class DocumentUsageType {
    @GraphQLDescription("Document is used by an expense.")
    EXPENSE,

    @GraphQLDescription("Document is used by an income.")
    INCOME,

    @GraphQLDescription("Document is used by an invoice.")
    INVOICE,

    @GraphQLDescription("Document is used by an income tax payment.")
    INCOME_TAX_PAYMENT,
}
