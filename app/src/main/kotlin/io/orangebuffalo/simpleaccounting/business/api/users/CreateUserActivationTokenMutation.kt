package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.users.UserActivationTokenCreationException
import org.springframework.stereotype.Component

@Component
class CreateUserActivationTokenMutation(
    private val userService: PlatformUsersService,
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription(
        "Creates a new activation token for the specified user. " +
                "If an existing token is present, it will be replaced. " +
                "Only accessible by admin users."
    )
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    @BusinessError(
        exceptionClass = UserActivationTokenCreationException.UserAlreadyActivatedException::class,
        errorCode = "USER_ALREADY_ACTIVATED",
        errorCodeDescription = "The user is already activated and cannot receive an activation token.",
    )
    suspend fun createUserActivationToken(
        @GraphQLDescription("The ID of the user to create the activation token for.")
        userId: Long,
    ): UserActivationTokenGqlDto {
        return userService.createUserActivationToken(userId).mapToGqlDto()
    }
}
