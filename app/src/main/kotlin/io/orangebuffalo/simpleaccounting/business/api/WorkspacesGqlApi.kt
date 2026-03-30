package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadCategoryById
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadExpensesByWorkspaceId
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesService
import io.orangebuffalo.simpleaccounting.business.customers.CustomersService
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.decodeCursor
import io.orangebuffalo.simpleaccounting.infra.graphql.getBean
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
    ): ConnectionGqlDto<WorkspaceGqlDto> {
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
            id = workspace.id!!.toInt(),
            name = workspace.name,
            defaultCurrency = workspace.defaultCurrency,
        )
    }
}

@GraphQLName("Workspace")
@GraphQLDescription("Workspace of a user.")
data class WorkspaceGqlDto(
    @GraphQLDescription("ID of the workspace.")
    val id: Int,

    @GraphQLDescription("Name of the workspace.")
    val name: String,

    @GraphQLDescription("Default currency of the workspace.")
    val defaultCurrency: String,
) {
    @GraphQLDescription("Categories in this workspace with cursor-based pagination.")
    suspend fun categories(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<CategoryGqlDto> {
        val cursorPage = decodeCursor(after)
        return env.graphQlContext.getBean<CategoriesService>().getCategoriesPaginated(
            workspaceId = id.toLong(),
            first = first,
            cursorPage = cursorPage,
        )
    }

    @GraphQLDescription("Expenses in this workspace.")
    fun expenses(env: DataFetchingEnvironment) = env.loadExpensesByWorkspaceId(id.toLong())

    @GraphQLDescription("Documents in this workspace with cursor-based pagination.")
    suspend fun documents(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<DocumentGqlDto> {
        val cursorPage = decodeCursor(after)
        return env.graphQlContext.getBean<DocumentsService>().getDocumentsPaginated(
            workspaceId = id.toLong(),
            first = first,
            cursorPage = cursorPage,
        )
    }

    @GraphQLDescription("Customers in this workspace with cursor-based pagination.")
    suspend fun customers(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<CustomerGqlDto> {
        val cursorPage = decodeCursor(after)
        return env.graphQlContext.getBean<CustomersService>().getCustomersPaginated(
            workspaceId = id.toLong(),
            first = first,
            cursorPage = cursorPage,
        )
    }
}

@GraphQLName("Expense")
@GraphQLDescription("Business expense.")
data class ExpenseGqlDto(
    @GraphQLDescription("Title of the expense.")
    val title: String,

    @property:GraphQLIgnore val categoryId: Long?,
) {
    @GraphQLDescription("Category of the expense.")
    fun category(env: DataFetchingEnvironment): CompletableFuture<CategoryGqlDto?>? {
        val catId = categoryId ?: return null
        return env.loadCategoryById(catId)
    }
}
