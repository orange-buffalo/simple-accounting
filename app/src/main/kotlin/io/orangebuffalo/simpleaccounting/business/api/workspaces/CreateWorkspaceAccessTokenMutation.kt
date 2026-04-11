package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CreateWorkspaceAccessTokenMutation(
    private val workspacesService: WorkspacesService,
    private val workspaceAccessTokensService: WorkspaceAccessTokensService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new access token for sharing workspace access.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createWorkspaceAccessToken(
        @GraphQLDescription("ID of the workspace to create the token for.")
        workspaceId: Long,
        @GraphQLDescription("The expiration time of the token.")
        validTill: Instant,
    ): WorkspaceAccessTokenGqlDto {
        val workspace = workspacesService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.ADMIN)
        return workspaceAccessTokensService.createAccessToken(workspace, validTill)
            .toWorkspaceAccessTokenGqlDto()
    }
}
