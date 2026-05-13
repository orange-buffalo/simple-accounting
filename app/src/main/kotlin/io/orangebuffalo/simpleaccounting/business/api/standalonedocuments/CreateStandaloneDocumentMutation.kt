package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocumentsService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CreateStandaloneDocumentMutation(
    private val standaloneDocumentsService: StandaloneDocumentsService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new standalone document in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createStandaloneDocument(
        @GraphQLDescription("ID of the workspace to create the standalone document in.")
        workspaceId: String,
        @GraphQLDescription("Title of the standalone document.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("ID of the linked document.")
        documentId: String,
    ): StandaloneDocumentGqlDto = standaloneDocumentsService.createStandaloneDocument(
        workspaceId = workspaceId,
        standaloneDocument = StandaloneDocument(
            title = title,
            documentId = documentId,
        ),
    ).toStandaloneDocumentGqlDto()
}
