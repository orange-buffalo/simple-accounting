package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument

@GraphQLName("StandaloneDocument")
@GraphQLDescription("A standalone document in a workspace.")
data class StandaloneDocumentGqlDto(
    @GraphQLDescription("ID of the standalone document.")
    val id: String,

    @GraphQLDescription("Version of the standalone document state.")
    val version: Int,

    @GraphQLDescription("Title of the standalone document.")
    val title: String,

    @GraphQLDescription("ID of the linked document.")
    val documentId: String,
)

fun StandaloneDocument.toStandaloneDocumentGqlDto() = StandaloneDocumentGqlDto(
    id = id!!,
    version = version!!,
    title = title,
    documentId = documentId,
)
