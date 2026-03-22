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
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.PageInfoGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.decodeCursor
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
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
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

/**
 * A paginated connection following the
 * [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm).
 * See also [GraphQL Pagination Guide](https://graphql.org/learn/pagination/).
 */
@GraphQLDescription("A paginated connection of workspaces following the GraphQL Cursor Connections Specification.")
data class WorkspacesConnectionGqlDto(
    @GraphQLDescription("The list of edges in the current page.")
    override val edges: List<WorkspaceEdgeGqlDto>,

    @GraphQLDescription("Pagination information about the current page.")
    override val pageInfo: PageInfoGqlDto,

    @GraphQLDescription("The total number of items in the connection across all pages.")
    override val totalCount: Int,
) : ConnectionGqlDtoBase()

@GraphQLDescription("An edge in a workspaces connection.")
data class WorkspaceEdgeGqlDto(
    @GraphQLDescription("The cursor of this edge, which can be used for pagination.")
    override val cursor: String,

    @GraphQLDescription("The workspace at the end of this edge.")
    override val node: WorkspaceGqlDto,
) : EdgeGqlDtoBase()

/**
 * Base class for connection DTOs ensuring a consistent structure across all paginated connections.
 * Concrete connection types must override all properties.
 *
 * Note: graphql-kotlin does not support generic type parameters in schema generation,
 * so this uses abstract properties to enforce the contract at compile time.
 * The base class is excluded from the schema via `@GraphQLIgnore`.
 */
@GraphQLIgnore
abstract class ConnectionGqlDtoBase {
    abstract val edges: List<*>
    abstract val pageInfo: PageInfoGqlDto
    abstract val totalCount: Int
}

/**
 * Base class for edge DTOs ensuring a consistent structure across all paginated edges.
 */
@GraphQLIgnore
abstract class EdgeGqlDtoBase {
    abstract val cursor: String
    abstract val node: Any
}

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
