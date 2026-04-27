package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class WorkspacesQuery(
    private val paginationService: GraphqlPaginationService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns all workspaces accessible by the current user with cursor-based pagination.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun workspaces(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int? = null,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
    ): ConnectionGqlDto<WorkspaceGqlDto> {
        val workspace = Tables.WORKSPACE
        return paginationService.forTable(workspace)
            .applyCurrentUserFiltering { user -> workspace.ownerId.eq(user.id) }
            .page(first ?: GraphqlPaginationConstants.PAGE_SIZE_DEFAULT.toInt(), after) { record ->
                WorkspaceGqlDto(
                    id = record[workspace.id]!!,
                    name = record[workspace.name]!!,
                    defaultCurrency = record[workspace.defaultCurrency]!!,
                )
            }
    }
}
