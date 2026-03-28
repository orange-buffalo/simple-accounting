package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.EdgeGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.PageInfoGqlDto

@GraphQLName("DocumentsConnection")
@GraphQLDescription("A paginated connection of documents following the GraphQL Cursor Connections Specification.")
data class DocumentsConnectionGqlDto(
    @GraphQLDescription("The list of edges in the current page.")
    override val edges: List<DocumentEdgeGqlDto>,

    @GraphQLDescription("Pagination information about the current page.")
    override val pageInfo: PageInfoGqlDto,

    @GraphQLDescription("The total number of items in the connection across all pages.")
    override val totalCount: Int,
) : ConnectionGqlDto

@GraphQLName("DocumentEdge")
@GraphQLDescription("An edge in a documents connection.")
data class DocumentEdgeGqlDto(
    @GraphQLDescription("The cursor of this edge, which can be used for pagination.")
    override val cursor: String,

    @GraphQLDescription("The document at the end of this edge.")
    override val node: DocumentGqlDto,
) : EdgeGqlDto

@GraphQLName("Document")
@GraphQLDescription("A document in a workspace.")
data class DocumentGqlDto(
    @GraphQLDescription("ID of the document.")
    val id: Int,

    @GraphQLDescription("Version of the document for optimistic locking.")
    val version: Int,

    @GraphQLDescription("Name of the document.")
    val name: String,

    @GraphQLDescription("Time when the document was uploaded, as ISO 8601 timestamp.")
    val timeUploaded: String,

    @GraphQLDescription("Size of the document in bytes.")
    val sizeInBytes: Int?,

    @GraphQLDescription("ID of the storage where the document is stored.")
    val storageId: String,

    @GraphQLDescription("MIME type of the document.")
    val mimeType: String,

    @GraphQLDescription("Entities that use this document.")
    val usedBy: List<DocumentUsageGqlDto>,
)

@GraphQLName("DocumentUsage")
@GraphQLDescription("Describes usage of a document by another entity.")
data class DocumentUsageGqlDto(
    @GraphQLDescription("Type of entity using the document.")
    val type: DocumentUsageType,

    @GraphQLDescription("ID of the entity using the document.")
    val relatedEntityId: Int,
)

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
