package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.stereotype.Component

@Component
class RevokeWorkspaceAccessTokenMutation(
    private val workspacesService: WorkspacesService,
    private val workspaceAccessTokensService: WorkspaceAccessTokensService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Revokes and removes a workspace access token.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun revokeWorkspaceAccessToken(
        @GraphQLDescription("ID of the workspace access token to revoke.")
        accessTokenId: String,
    ): Boolean {
        val accessToken = workspaceAccessTokensService.getToken(accessTokenId)
        workspacesService.getAccessibleWorkspace(accessToken.workspaceId, WorkspaceAccessMode.ADMIN)
        workspaceAccessTokensService.deleteAccessToken(accessToken)
        return true
    }
}
