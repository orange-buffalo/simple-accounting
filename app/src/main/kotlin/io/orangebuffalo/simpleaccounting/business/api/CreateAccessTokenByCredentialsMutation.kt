package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.authentication.AccountIsTemporaryLockedException
import io.orangebuffalo.simpleaccounting.business.security.authentication.AccountLockedErrorExtensions
import io.orangebuffalo.simpleaccounting.business.security.authentication.LoginUnavailableException
import io.orangebuffalo.simpleaccounting.business.security.authentication.UserNotActivatedException
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.TOKEN_LIFETIME_IN_DAYS
import io.orangebuffalo.simpleaccounting.infra.getServerWebExchange
import jakarta.validation.constraints.NotBlank
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Component
@Validated
class CreateAccessTokenByCredentialsMutation(
    private val authenticationManager: ReactiveAuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokensService: RefreshTokensService,
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription(
        "Authenticates a user by username and password credentials and returns an access token. " +
                "Optionally issues a refresh token cookie for persistent sessions."
    )
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    @BusinessError(
        exceptionClass = BadCredentialsException::class,
        errorCode = "BAD_CREDENTIALS",
        description = "The provided credentials are invalid.",
    )
    @BusinessError(
        exceptionClass = UserNotActivatedException::class,
        errorCode = "USER_NOT_ACTIVATED",
        description = "The user account has not been activated yet.",
    )
    @BusinessError(
        exceptionClass = AccountIsTemporaryLockedException::class,
        errorCode = "ACCOUNT_LOCKED",
        description = "The account is temporarily locked due to too many failed login attempts. " +
                "The error extensions will include 'lockExpiresInSec' with the remaining lock duration in seconds.",
        extensionsType = AccountLockedErrorExtensions::class,
    )
    @BusinessError(
        exceptionClass = LoginUnavailableException::class,
        errorCode = "LOGIN_NOT_AVAILABLE",
        description = "Login is temporarily unavailable due to too many concurrent authentication requests for this user.",
    )
    suspend fun createAccessTokenByCredentials(
        @GraphQLDescription("The username of the user.")
        @NotBlank
        userName: String,
        @GraphQLDescription("The password of the user.")
        @NotBlank
        password: String,
        @GraphQLDescription(
            "Whether to issue a refresh token cookie for persistent sessions. " +
                    "Defaults to false if not provided."
        )
        issueRefreshTokenCookie: Boolean? = null,
    ): CreateAccessTokenByCredentialsResponse {
        val authenticationToken = UsernamePasswordAuthenticationToken(userName, password)
        val authentication = authenticationManager.authenticate(authenticationToken).awaitSingle()
        val principal = authentication.principal as SecurityPrincipal
        val jwtToken = jwtService.buildJwtToken(principal)

        if (issueRefreshTokenCookie == true) {
            val exchange = getServerWebExchange()
            val refreshToken = refreshTokensService.generateRefreshToken(principal.userName)
            exchange.response.addCookie(
                ResponseCookie
                    .from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .sameSite("Strict")
                    .path("/api")
                    // todo #67: secure based on configuration
                    .maxAge(Duration.ofDays(TOKEN_LIFETIME_IN_DAYS))
                    .build()
            )
        }

        return CreateAccessTokenByCredentialsResponse(accessToken = jwtToken)
    }

    @GraphQLDescription("Response for the createAccessTokenByCredentials mutation.")
    data class CreateAccessTokenByCredentialsResponse(
        @param:GraphQLDescription("The JWT access token for the authenticated user.")
        val accessToken: String,
    )
}
