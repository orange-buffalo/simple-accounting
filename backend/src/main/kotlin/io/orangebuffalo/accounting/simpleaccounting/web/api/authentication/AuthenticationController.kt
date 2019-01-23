package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import io.orangebuffalo.accounting.simpleaccounting.services.integration.awaitMono
import io.orangebuffalo.accounting.simpleaccounting.services.security.core.DelegatingReactiveAuthenticationManager
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtService
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.RefreshAuthenticationToken
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.RefreshTokenService
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.TOKEN_LIFETIME_IN_DAYS
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.time.Duration
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("api/v1/auth")
class AuthenticationController(
    private val authenticationManager: DelegatingReactiveAuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService
) {

    @PostMapping("login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest) = GlobalScope.mono {
        val authenticationToken = UsernamePasswordAuthenticationToken(loginRequest.userName, loginRequest.password)
        val authentication = authenticationManager.authenticate(authenticationToken).awaitMono()
        val userDetails = authentication.principal as UserDetails
        val jwtToken = jwtService.buildJwtToken(userDetails)

        val response = ResponseEntity.ok()

        if (loginRequest.rememberMe) {
            response
                .withRefreshTokenCookie(
                    refreshTokenService.generateRefreshToken(userDetails.username),
                    Duration.ofDays(TOKEN_LIFETIME_IN_DAYS)
                )
        }

        response.body(TokenResponse(jwtToken))
    }

    @PostMapping("token")
    fun refreshToken(
        @CookieValue("refreshToken", required = false) refreshToken: String?,
        authentication: Authentication?
    ) = GlobalScope.mono {

        val authenticatedAuth = when {
            authentication != null && authentication.isAuthenticated -> authentication
            refreshToken != null -> {
                val authenticationToken = RefreshAuthenticationToken(refreshToken)
                authenticationManager.authenticate(authenticationToken).awaitMono()
            }
            else -> throw InsufficientAuthenticationException("Not authenticated")
        }

        val userDetails = authenticatedAuth.principal as UserDetails
        TokenResponse(jwtService.buildJwtToken(userDetails))
    }

    @PostMapping("logout")
    fun logout() = GlobalScope.mono {
        ResponseEntity.ok().withRefreshTokenCookie(null, Duration.ZERO).body("")
    }

    private fun ResponseEntity.BodyBuilder.withRefreshTokenCookie(
        value: String?,
        maxAge: Duration
    ): ResponseEntity.BodyBuilder {
        return header(
            HttpHeaders.SET_COOKIE,
            ResponseCookie
                .from("refreshToken", value ?: "")
                .httpOnly(true)
                .sameSite("Strict")
                .path("/api/v1/auth/token")
                // todo secure based on configuration
                .maxAge(maxAge)
                .build()
                .toString()
        )
    }
}

data class LoginRequest(
    @field:NotBlank val userName: String,
    @field:NotBlank val password: String,
    val rememberMe: Boolean = false
)

data class TokenResponse(
    val token: String
)