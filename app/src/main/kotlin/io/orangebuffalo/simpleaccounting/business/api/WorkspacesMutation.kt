package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class WorkspacesMutation(
    private val workspacesService: WorkspacesService,
    private val platformUsersService: PlatformUsersService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new workspace for the current user.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createWorkspace(
        @GraphQLDescription("Name of the workspace.")
        @NotBlank
        @Size(max = 255)
        name: String,
        @GraphQLDescription("Default currency code of the workspace (ISO 4217, 3 characters).")
        @NotBlank
        @Size(max = 3)
        defaultCurrency: String,
    ): WorkspaceGqlDto {
        val currentUser = platformUsersService.getCurrentUser()
        val workspace = workspacesService.createWorkspace(
            Workspace(
                name = name,
                defaultCurrency = defaultCurrency,
                ownerId = currentUser.id!!,
            )
        )
        return workspace.toWorkspaceGqlDto()
    }

    @Suppress("unused")
    @GraphQLDescription("Updates an existing workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editWorkspace(
        @GraphQLDescription("ID of the workspace to update.")
        id: Long,
        @GraphQLDescription("New name of the workspace.")
        @NotBlank
        @Size(max = 255)
        name: String,
    ): WorkspaceGqlDto {
        val workspace = workspacesService.getAccessibleWorkspace(id, WorkspaceAccessMode.ADMIN)
        workspace.name = name
        return workspacesService.save(workspace).toWorkspaceGqlDto()
    }
}

private fun Workspace.toWorkspaceGqlDto() = WorkspaceGqlDto(
    id = id!!,
    name = name,
    defaultCurrency = defaultCurrency,
)
