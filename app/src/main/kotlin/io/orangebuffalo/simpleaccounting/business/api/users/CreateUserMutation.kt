package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.users.UserCreationException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CreateUserMutation(
    private val platformUsersService: PlatformUsersService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new user account. Only accessible by admin users.")
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    @BusinessError(
        exceptionClass = UserCreationException.UserAlreadyExistsException::class,
        errorCode = "USER_ALREADY_EXISTS",
        errorCodeDescription = "A user with the given username already exists.",
    )
    suspend fun createUser(
        @GraphQLDescription("Username / login for the new user.")
        @NotBlank
        @Size(max = 255)
        userName: String,
        @GraphQLDescription("Whether the new user should have admin privileges.")
        admin: Boolean,
    ): PlatformUserGqlDto {
        return platformUsersService.createUser(
            userName = userName,
            isAdmin = admin,
        ).toPlatformUserGqlDto()
    }
}
