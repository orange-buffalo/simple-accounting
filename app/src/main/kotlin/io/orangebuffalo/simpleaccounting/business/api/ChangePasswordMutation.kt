package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.security.authentication.AuthenticationService
import org.springframework.stereotype.Component

@Component
class ChangePasswordMutation(
    private val authenticationService: AuthenticationService,
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription("Changes the password of the current user.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    suspend fun changePassword(
        @GraphQLDescription("The current password of the user.")
        currentPassword: String,
        @GraphQLDescription("The new password to set for the user.")
        newPassword: String
    ): ChangePasswordResponse {
        authenticationService.changeCurrentUserPassword(currentPassword, newPassword)
        return ChangePasswordResponse()
    }

    @GraphQLDescription(
        "Response for the changePassword mutation. " +
                "Always succeeds if no error is returned by standard GraphQL error response structure."
    )
    data class ChangePasswordResponse(
        val success: Boolean = true,
    )
}
