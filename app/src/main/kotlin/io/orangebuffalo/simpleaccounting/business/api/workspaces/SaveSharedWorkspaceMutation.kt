package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.workspaces.InvalidWorkspaceAccessTokenException
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class SaveSharedWorkspaceMutation(
    private val workspacesService: WorkspacesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Saves a shared workspace to the current user's list using an access token.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    @BusinessError(
        exceptionClass = InvalidWorkspaceAccessTokenException::class,
        errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
        errorCodeDescription = "The provided workspace access token is not valid (unknown, expired, or revoked).",
    )
    suspend fun saveSharedWorkspace(
        @GraphQLDescription("The workspace access token.")
        @NotBlank
        token: String,
    ): WorkspaceGqlDto = workspacesService.saveSharedWorkspace(token).toWorkspaceGqlDto()
}
