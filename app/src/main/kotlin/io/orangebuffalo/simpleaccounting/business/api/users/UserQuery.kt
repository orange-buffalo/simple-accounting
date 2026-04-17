package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import org.springframework.stereotype.Component

@Component
class UserQuery(
    private val platformUsersService: PlatformUsersService,
) : Query {

    @Suppress("unused")
    @GraphQLDescription("Returns the user with the given ID.")
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    suspend fun user(
        @GraphQLDescription("ID of the user.") id: Long,
    ): PlatformUserGqlDto {
        return platformUsersService.getUserByUserId(id).toPlatformUserGqlDto()
    }
}
