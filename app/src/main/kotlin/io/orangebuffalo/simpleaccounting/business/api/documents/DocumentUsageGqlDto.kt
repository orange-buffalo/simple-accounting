package io.orangebuffalo.simpleaccounting.business.api.documents

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("DocumentUsage")
@GraphQLDescription("Describes usage of a document by another entity.")
data class DocumentUsageGqlDto(
    @GraphQLDescription("Type of entity using the document.")
    val type: DocumentUsageType,

    @GraphQLDescription("ID of the entity using the document.")
    val relatedEntityId: Long,

    @GraphQLDescription("Display name of the entity using the document.")
    val displayName: String,
)
