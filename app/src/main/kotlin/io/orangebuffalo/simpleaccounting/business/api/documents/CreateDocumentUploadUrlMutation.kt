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
class CreateDocumentUploadUrlMutation(
    private val documentsService: DocumentsService,
    private val simpleAccountingProperties: SimpleAccountingProperties,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription(
        "Creates a short-lived upload URL for a document (token expires in 2 minutes). " +
                "The URL is absolute and can be used directly by the client to upload a document. " +
                "The URL does not require authentication. " +
                "The upload should be done as a multipart/form-data POST request, " +
                "with the file content in a part named as specified in `filePartName`. " +
                "The response of the upload request is a JSON object matching the `Document` type in this schema."
    )
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createDocumentUploadUrl(
        @GraphQLDescription("ID of the workspace to upload the document to.")
        workspaceId: Long,
    ): CreateDocumentUploadUrlResponse {
        val token = documentsService.getUploadToken(workspaceId)
        val baseUrl = simpleAccountingProperties.publicUrl.trimEnd('/')
        val uploadUrl = "$baseUrl/api/documents/upload/$token"
        return CreateDocumentUploadUrlResponse(url = uploadUrl, filePartName = "file")
    }

    @GraphQLDescription("Response containing the temporary upload URL for a document.")
    data class CreateDocumentUploadUrlResponse(
        @GraphQLDescription(
            "Absolute URL to upload the document content via multipart/form-data POST. " +
                    "The URL is temporary and will expire."
        )
        val url: String,

        @GraphQLDescription(
            "The name of the multipart form-data part that should contain the file content."
        )
        val filePartName: String,
    )
}
