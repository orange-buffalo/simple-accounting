package io.orangebuffalo.simpleaccounting.business.api.documents

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.documents.Document
import java.time.Instant

@GraphQLName("Document")
@GraphQLDescription("A document in a workspace.")
data class DocumentGqlDto(
    @GraphQLDescription("ID of the document.")
    val id: Long,

    @GraphQLDescription("Version of the document for optimistic locking.")
    val version: Int,

    @GraphQLDescription("Name of the document.")
    val name: String,

    @GraphQLDescription("Time when the document was uploaded, as ISO 8601 timestamp.")
    val timeUploaded: Instant,

    @GraphQLDescription("Size of the document in bytes.")
    val sizeInBytes: Long?,

    @GraphQLDescription("ID of the storage where the document is stored.")
    val storageId: String,

    @GraphQLDescription("MIME type of the document.")
    val mimeType: String,

    @GraphQLDescription("Entities that use this document.")
    val usedBy: List<DocumentUsageGqlDto>,
)

fun Document.toGqlDto() = DocumentGqlDto(
    id = id!!,
    version = version!!,
    name = name,
    timeUploaded = timeUploaded,
    sizeInBytes = sizeInBytes,
    storageId = storageId,
    mimeType = mimeType,
    usedBy = emptyList(),
)
