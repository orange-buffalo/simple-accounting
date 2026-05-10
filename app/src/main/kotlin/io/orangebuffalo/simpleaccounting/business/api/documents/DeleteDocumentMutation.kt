package io.orangebuffalo.simpleaccounting.business.api.documents

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.documents.DocumentIsUsedException
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class DeleteDocumentMutation(
    private val documentsService: DocumentsService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Deletes an unused document from the database and the backing document storage.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    @BusinessError(
        exceptionClass = DocumentIsUsedException::class,
        errorCode = "DOCUMENT_IS_USED",
        errorCodeDescription = "The document is attached to another entity and cannot be deleted.",
    )
    suspend fun deleteDocument(
        @GraphQLDescription("ID of the workspace the document belongs to.")
        workspaceId: String,
        @GraphQLDescription("ID of the document to delete.")
        documentId: String,
    ): Boolean {
        documentsService.deleteDocument(workspaceId, documentId)
        return true
    }
}
