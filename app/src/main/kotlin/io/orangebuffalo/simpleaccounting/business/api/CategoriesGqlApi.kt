package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesService
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.decodeCursor
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CategoriesQuery(
    private val categoriesService: CategoriesService,
    private val workspacesService: WorkspacesService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns all categories in the workspace accessible by the current user with cursor-based pagination.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun categories(
        @GraphQLDescription("ID of the workspace.") workspaceId: Int,
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
    ): ConnectionGqlDto<CategoryGqlDto> {
        workspacesService.getAccessibleWorkspace(workspaceId.toLong(), WorkspaceAccessMode.READ_ONLY)
        val cursorPage = decodeCursor(after)
        return categoriesService.getCategoriesPaginated(
            workspaceId = workspaceId.toLong(),
            first = first,
            cursorPage = cursorPage,
        )
    }
}

@GraphQLName("Category")
@GraphQLDescription("Category of incomes or expenses.")
data class CategoryGqlDto(
    @GraphQLDescription("ID of the category.")
    val id: Int,

    @GraphQLDescription("Name of the category.")
    val name: String,

    @GraphQLDescription("Description of the category.")
    val description: String?,

    @GraphQLDescription("Whether this category applies to incomes.")
    val income: Boolean,

    @GraphQLDescription("Whether this category applies to expenses.")
    val expense: Boolean,
)
