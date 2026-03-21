package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadCategoriesByWorkspaceId
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadCategoryById
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadExpensesByWorkspaceId
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.PageInfoGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.buildConnection
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.decodeCursor
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.util.concurrent.CompletableFuture

@Component
@Validated
class WorkspacesQuery(
    private val workspacesService: WorkspacesService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns all workspaces accessible by the current user with cursor-based pagination.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun workspaces(
        @GraphQLDescription("The maximum number of items to return.") @Min(1) @Max(500) first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
    ): WorkspacesConnectionGqlDto {
        val principal = ensureRegularUserPrincipal()
        val cursorPage = decodeCursor(after)
        return workspacesService.getUserWorkspacesPaginated(
            userName = principal.userName,
            first = first,
            cursorPage = cursorPage,
        )
    }

    @Suppress("unused")
    @GraphQLDescription("Returns a workspace by its ID, if accessible by the current user.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_ACTOR)
    suspend fun workspace(
        @GraphQLDescription("ID of the workspace.") id: Int,
    ): WorkspaceGqlDto {
        val workspace = workspacesService.getAccessibleWorkspace(id.toLong(), WorkspaceAccessMode.READ_ONLY)
        return WorkspaceGqlDto(
            id = workspace.id!!,
            name = workspace.name,
        )
    }
}

@GraphQLDescription("A paginated connection of workspaces following the GraphQL Cursor Connections Specification.")
data class WorkspacesConnectionGqlDto(
    @GraphQLDescription("The list of edges in the current page.")
    val edges: List<WorkspaceEdgeGqlDto>,

    @GraphQLDescription("Pagination information about the current page.")
    val pageInfo: PageInfoGqlDto,

    @GraphQLDescription("The total number of items in the connection across all pages.")
    val totalCount: Int,
)

@GraphQLDescription("An edge in a workspaces connection.")
data class WorkspaceEdgeGqlDto(
    @GraphQLDescription("The cursor of this edge, which can be used for pagination.")
    val cursor: String,

    @GraphQLDescription("The workspace at the end of this edge.")
    val node: WorkspaceGqlDto,
)

@GraphQLDescription("Workspace of a user.")
data class WorkspaceGqlDto(
    @property:GraphQLIgnore val id: Long,

    @param:GraphQLDescription("Name of the workspace.")
    val name: String,
) {
    @GraphQLDescription("Categories in this workspace.")
    fun categories(env: DataFetchingEnvironment) = env.loadCategoriesByWorkspaceId(id)

    @GraphQLDescription("Expenses in this workspace.")
    fun expenses(env: DataFetchingEnvironment) = env.loadExpensesByWorkspaceId(id)
}

@GraphQLDescription("Category of incomes or expenses.")
data class CategoryGqlDto(
    @param:GraphQLDescription("Name of the category.")
    val name: String,
)

@GraphQLDescription("Business expense.")
data class ExpenseGqlDto(
    @param:GraphQLDescription("Title of the expense.")
    val title: String,

    @property:GraphQLIgnore val categoryId: Long?,
) {
    @GraphQLDescription("Category of the expense.")
    fun category(env: DataFetchingEnvironment): CompletableFuture<CategoryGqlDto?>? {
        val catId = categoryId ?: return null
        return env.loadCategoryById(catId)
    }
}
