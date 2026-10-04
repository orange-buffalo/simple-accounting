package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.workspaces.IncompatibleWorkspaceCurrencyException
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CreateWorkspaceMutation(
    private val workspacesService: WorkspacesService,
    private val platformUsersService: PlatformUsersService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new workspace for the current user.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    @BusinessError(
        exceptionClass = IncompatibleWorkspaceCurrencyException::class,
        errorCode = "INCOMPATIBLE_CURRENCY",
        errorCodeDescription = "The currency is not supported for the residency country.",
    )
    fun createWorkspace(
        @GraphQLDescription("Name of the workspace.")
        @NotBlank
        @Size(max = 255)
        name: String,
        @GraphQLDescription("Default currency code of the workspace (ISO 4217, 3 characters).")
        @NotBlank
        @Size(max = 3)
        defaultCurrency: String,
        @GraphQLDescription("Residency country of the workspace (ISO 3166-1 alpha-2).")
        @CountryCode
        @NotBlank
        residency: String,
    ): WorkspaceGqlDto {
        val currentUser = platformUsersService.getCurrentUser()
        val workspace = workspacesService.createWorkspace(
            Workspace(
                name = name,
                defaultCurrency = defaultCurrency,
                residency = residency,
                ownerId = currentUser.id!!,
            )
        )
        return workspace.toWorkspaceGqlDto()
    }
}
