package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocumentsService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class EditStandaloneDocumentMutation(
    private val standaloneDocumentsService: StandaloneDocumentsService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing standalone document in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editStandaloneDocument(
        @GraphQLDescription("ID of the workspace the standalone document belongs to.")
        workspaceId: String,
        @GraphQLDescription("ID of the standalone document to update.")
        id: String,
        @GraphQLDescription("New title of the standalone document.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("New ID of the linked document.")
        documentId: String,
    ): StandaloneDocumentGqlDto {
        val standaloneDocument = standaloneDocumentsService.getStandaloneDocumentByIdAndWorkspaceId(id, workspaceId)
            ?: throw EntityNotFoundException("Standalone document $id is not found")

        return standaloneDocumentsService.saveStandaloneDocument(
            workspaceId = workspaceId,
            standaloneDocument = standaloneDocument.copy(
                title = title,
                documentId = documentId,
            ),
        ).toStandaloneDocumentGqlDto()
    }
}
