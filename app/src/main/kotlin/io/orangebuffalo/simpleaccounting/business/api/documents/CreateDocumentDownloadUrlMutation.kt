package io.orangebuffalo.simpleaccounting.business.api.documents

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CreateDocumentDownloadUrlMutation(
    private val documentsService: DocumentsService,
    private val simpleAccountingProperties: SimpleAccountingProperties,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription(
        "Creates a temporary download URL for a document. " +
                "The URL is absolute and can be used directly by the client to download the document content."
    )
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_ACTOR)
    suspend fun createDocumentDownloadUrl(
        @GraphQLDescription("ID of the workspace the document belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the document to create a download URL for.")
        documentId: Long,
    ): CreateDocumentDownloadUrlResponse {
        val token = documentsService.getDownloadToken(workspaceId, documentId)
        val baseUrl = simpleAccountingProperties.publicUrl.trimEnd('/')
        val downloadUrl = "$baseUrl/api/downloads?token=$token"
        return CreateDocumentDownloadUrlResponse(url = downloadUrl)
    }

    @GraphQLDescription("Response containing the temporary download URL for a document.")
    data class CreateDocumentDownloadUrlResponse(
        @GraphQLDescription("Absolute URL to download the document content. The URL is temporary and will expire.")
        val url: String,
    )
}
