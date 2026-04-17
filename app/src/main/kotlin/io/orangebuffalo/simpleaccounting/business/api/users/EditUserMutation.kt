package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.users.UserUpdateException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class EditUserMutation(
    private val platformUsersService: PlatformUsersService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing user's username. Only accessible by admin users.")
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    @BusinessError(
        exceptionClass = UserUpdateException.UserAlreadyExistsException::class,
        errorCode = "USER_ALREADY_EXISTS",
        errorCodeDescription = "A user with the given username already exists.",
    )
    suspend fun editUser(
        @GraphQLDescription("ID of the user to update.")
        id: Long,
        @GraphQLDescription("New username / login for the user.")
        @NotBlank
        @Size(max = 255)
        userName: String,
    ): PlatformUserGqlDto {
        val user = platformUsersService.getUserByUserId(id)
        user.userName = userName
        return platformUsersService.updateUser(user).toPlatformUserGqlDto()
    }
}
