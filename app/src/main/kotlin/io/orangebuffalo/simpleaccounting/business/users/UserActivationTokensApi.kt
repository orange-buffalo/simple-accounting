package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.infra.rest.errorhandling.ApiErrorMapping
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlinx.coroutines.delay
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * REST API for user activation tokens, including management and usage.
 */
@RestController
@RequestMapping("/api/user-activation-tokens")
class UserActivationTokensApi(
    private val userService: PlatformUsersService,
    private val userManagementProperties: UserManagementProperties,
) {

    /**
     * Retrieves user activation token by user id. There is always at most one token per user.
     *
     * This API is accessible only for users with ADMIN role.
     *
     * In case the token is not found, or is expired, 404 response is returned.
     */
    @GetMapping("users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun getTokenByUser(
        @PathVariable userId: Long
    ): UserActivationTokenDto {
        val token = userService.getUserActivationTokenForUser(userId)
            ?: throw EntityNotFoundException("User activation token not found for user with id $userId")
        return token.mapToUserActivationTokenDto()
    }

    /**
     * Retrieves user activation token by token value.
     *
     * In case the token is not found, or is expired, 404 response is returned.
     *
     * This API is accessible by anonymous users.
     */
    @GetMapping("{token}")
    suspend fun getToken(
        @PathVariable token: String
    ): UserActivationTokenDto {
        delay(userManagementProperties.activation.tokenVerificationBruteForceDelayInMs)
        val userActivationToken = userService.getUserActivationToken(token)
            ?: throw EntityNotFoundException("User activation token not found $token")
        return userActivationToken.mapToUserActivationTokenDto()
    }

    /**
     * Creates a new user activation token for the user with the specified id.
     * In case there is an existing token for the user, it is replaced with a new one.
     * In case user is already activated, 400 response is returned
     * with `UserAlreadyActivated` error.
     *
     * This API is accessible only for users with ADMIN role.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiErrorMapping(UserActivationTokenCreationException.UserAlreadyActivatedException::class, "UserAlreadyActivated")
    suspend fun createToken(@RequestBody @Valid request: CreateUserActivationTokenRequestDto): UserActivationTokenDto =
        userService.createUserActivationToken(request.userId).mapToUserActivationTokenDto()

    /**
     * Activates the user by the token and sets the user password.
     * The token is invalidated after successful activation. The user is activated only if the
     * token is valid and not expired, otherwise 400 response is returned.
     *
     * To verify the token before activation, use the `GET /api/user-activation-tokens/{token}` API.
     *
     * This API is accessible by anonymous users.
     */
    @PostMapping("{token}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiErrorMapping(UserActivationException.TokenExpiredException::class, "TokenExpired")
    suspend fun activateUser(
        @PathVariable token: String,
        @RequestBody @Valid request: UserActivationRequestDto
    ) {
        delay(userManagementProperties.activation.tokenVerificationBruteForceDelayInMs)
        userService.activateUser(token = token, password = request.password)
    }
}

/**
 * A user activation token. Single token is associated with a single user.
 * The token is used to activate the user and set the user password after creation.
 */
data class UserActivationTokenDto(
    /**
     * The token value.
     */
    var token: String,
    /**
     * The date and time when the token expires.
     */
    var expiresAt: Instant
)

/**
 * Request to create a new user activation token.
 */
data class CreateUserActivationTokenRequestDto(
    /**
     * The id of the user for which the token should be created.
     */
    @field:NotNull var userId: Long
)

/**
 * Request to activate the user by the token.
 */
data class UserActivationRequestDto(
    /**
     * The password to set for the user.
     */
    @field:NotBlank
    @field:Size(max = 100) var password: String,
)

private fun UserActivationToken.mapToUserActivationTokenDto() = UserActivationTokenDto(
    token = token,
    expiresAt = expiresAt,
)

