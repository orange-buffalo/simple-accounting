package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.users.UserActivationException
import io.orangebuffalo.simpleaccounting.business.users.UserManagementProperties
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class ActivateUserMutation(
    private val userService: PlatformUsersService,
    private val userManagementProperties: UserManagementProperties,
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription(
        "Activates a user account using the provided token and sets the user's password. " +
                "The token is invalidated after successful activation. " +
                "Accessible by anonymous users."
    )
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    @BusinessError(
        exceptionClass = UserActivationException.TokenExpiredException::class,
        errorCode = "TOKEN_EXPIRED",
        errorCodeDescription = "The provided activation token has expired.",
    )
    suspend fun activateUser(
        @GraphQLDescription("The activation token value.")
        token: String,
        @GraphQLDescription("The password to set for the user.")
        @NotBlank
        @Size(max = 100)
        password: String,
    ): ActivateUserResponse {
        delay(userManagementProperties.activation.tokenVerificationBruteForceDelayInMs)
        userService.activateUser(token = token, password = password)
        return ActivateUserResponse()
    }

    @GraphQLDescription(
        "Response for the activateUser mutation. " +
                "Always succeeds if no error is returned by the standard GraphQL error response structure."
    )
    data class ActivateUserResponse(
        val success: Boolean = true,
    )
}
