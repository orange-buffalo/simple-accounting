package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.security.authentication.AuthenticationService
import io.orangebuffalo.simpleaccounting.business.security.authentication.PasswordChangeException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class ChangePasswordMutation(
    private val authenticationService: AuthenticationService,
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription("Changes the password of the current user.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    @BusinessError(
        exceptionClass = PasswordChangeException.InvalidCurrentPasswordException::class,
        errorCode = "CURRENT_PASSWORD_MISMATCH",
        description = "The provided current password does not match the user's actual password.",
    )
    @BusinessError(
        exceptionClass = PasswordChangeException.TransientUserException::class,
        errorCode = "TRANSIENT_USER",
        description = "Cannot change password for a transient user (e.g., shared workspace token user).",
    )
    suspend fun changePassword(
        @GraphQLDescription("The current password of the user.")
        @NotBlank
        @Size(max = 100)
        currentPassword: String,
        @GraphQLDescription("The new password to set for the user.")
        @NotBlank
        @Size(max = 100)
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
