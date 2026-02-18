package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.CategoriesByWorkspaceIdDataLoader
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.CategoryByIdDataLoader
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.ExpensesByWorkspaceIdDataLoader
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.load
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class WorkspacesQuery(
    private val workspacesService: WorkspacesService,
    private val categoriesByWorkspaceIdDataLoader: CategoriesByWorkspaceIdDataLoader,
    private val expensesByWorkspaceIdDataLoader: ExpensesByWorkspaceIdDataLoader,
    private val categoryByIdDataLoader: CategoryByIdDataLoader,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns all workspaces accessible by the current user.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun workspaces(): List<WorkspaceGqlDto> {
        val principal = ensureRegularUserPrincipal()
        return workspacesService.getUserWorkspaces(principal.userName).map { workspace ->
            WorkspaceGqlDto(
                id = workspace.id!!,
                name = workspace.name,
                categoriesByWorkspaceIdDataLoader = categoriesByWorkspaceIdDataLoader,
                expensesByWorkspaceIdDataLoader = expensesByWorkspaceIdDataLoader,
                categoryByIdDataLoader = categoryByIdDataLoader,
            )
        }
    }
}

@GraphQLDescription("Workspace of a user.")
data class WorkspaceGqlDto(
    @property:GraphQLIgnore val id: Long,

    @param:GraphQLDescription("Name of the workspace.")
    val name: String,

    @property:GraphQLIgnore val categoriesByWorkspaceIdDataLoader: CategoriesByWorkspaceIdDataLoader,
    @property:GraphQLIgnore val expensesByWorkspaceIdDataLoader: ExpensesByWorkspaceIdDataLoader,
    @property:GraphQLIgnore val categoryByIdDataLoader: CategoryByIdDataLoader,
) {
    @GraphQLDescription("Categories in this workspace.")
    fun categories(env: DataFetchingEnvironment) = categoriesByWorkspaceIdDataLoader.load(env, id)

    @GraphQLDescription("Expenses in this workspace.")
    fun expenses(env: DataFetchingEnvironment) = expensesByWorkspaceIdDataLoader.load(env, id)
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
    @property:GraphQLIgnore val categoryByIdDataLoader: CategoryByIdDataLoader,
) {
    @GraphQLDescription("Category of the expense.")
    fun category(env: DataFetchingEnvironment): CompletableFuture<CategoryGqlDto?>? {
        val catId = categoryId ?: return null
        return categoryByIdDataLoader.load(env, catId)
    }
}
