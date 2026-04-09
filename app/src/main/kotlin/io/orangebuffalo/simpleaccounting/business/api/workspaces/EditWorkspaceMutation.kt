package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class EditWorkspaceMutation(
    private val workspacesService: WorkspacesService,
) : Mutation {

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
