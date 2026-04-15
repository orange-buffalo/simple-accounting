package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.users.UserManagementProperties
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component

@Component
class TokenByValueQuery(
    private val userService: PlatformUsersService,
    private val userManagementProperties: UserManagementProperties,
) : Query {
    @Suppress("unused")
    @GraphQLDescription(
        "Retrieves an activation token by its value. " +
                "Returns null if the token does not exist or has expired. " +
                "Accessible by anonymous users."
    )
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    suspend fun tokenByValue(
        @GraphQLDescription("The activation token value.")
        token: String,
    ): UserActivationTokenGqlDto? {
        delay(userManagementProperties.activation.tokenVerificationBruteForceDelayInMs)
        return userService.getUserActivationToken(token)?.mapToGqlDto()
    }
}
