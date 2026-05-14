package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocumentsService
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class RemoveStandaloneDocumentMutation(
    private val standaloneDocumentsService: StandaloneDocumentsService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Removes a standalone document from the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun removeStandaloneDocument(
        @GraphQLDescription("ID of the workspace the standalone document belongs to.")
        workspaceId: String,
        @GraphQLDescription("ID of the standalone document to remove.")
        standaloneDocumentId: String,
        @GraphQLDescription("Whether to also delete the linked document when it is no longer used. Defaults to true when omitted.")
        removeDocumentIfUnused: Boolean? = null,
    ): Boolean {
        standaloneDocumentsService.removeStandaloneDocument(
            workspaceId = workspaceId,
            standaloneDocumentId = standaloneDocumentId,
            removeDocumentIfUnused = removeDocumentIfUnused ?: true,
        ) ?: throw EntityNotFoundException("Standalone document $standaloneDocumentId is not found")

        return true
    }
}
