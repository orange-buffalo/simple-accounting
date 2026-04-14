package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import org.springframework.stereotype.Component

@Component
class TokenByUserQuery(
    private val userService: PlatformUsersService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription(
        "Retrieves the activation token for a user by their ID. " +
                "Returns null if the token does not exist or has expired. " +
                "Only accessible by admin users."
    )
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    suspend fun tokenByUser(
        @GraphQLDescription("The ID of the user to retrieve the activation token for.")
        userId: Long,
    ): UserActivationTokenGqlDto? {
        return userService.getUserActivationTokenForUser(userId)?.mapToGqlDto()
    }
}
