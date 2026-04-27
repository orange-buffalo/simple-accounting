package io.orangebuffalo.simpleaccounting.business.api.users

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
class UsersQuery(
    private val paginationService: GraphqlPaginationService,
) : Query {

    @Suppress("unused")
    @GraphQLDescription("Returns all users with cursor-based pagination. Only accessible by admin users.")
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    suspend fun users(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int? = null,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        @GraphQLDescription("Optional free-text search filter applied to the username.") freeSearchText: String? = null,
    ): ConnectionGqlDto<PlatformUserGqlDto> {
        val user = Tables.PLATFORM_USER
        return paginationService.forTable(user)
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(user.userName.containsIgnoreCase(freeSearchText))
                }
            }
            .page(first ?: GraphqlPaginationConstants.PAGE_SIZE_DEFAULT.toInt(), after) { record ->
                PlatformUserGqlDto(
                    id = record[user.id]!!,
                    userName = record[user.userName]!!,
                    admin = record[user.isAdmin]!!,
                    activated = record[user.activated]!!,
                )
            }
    }
}
