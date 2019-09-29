package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessTokenService
import io.orangebuffalo.accounting.simpleaccounting.services.integration.awaitMono
import io.orangebuffalo.accounting.simpleaccounting.services.security.SecurityPrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.security.core.DelegatingReactiveAuthenticationManager
import io.orangebuffalo.accounting.simpleaccounting.services.security.createTransientUserPrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtService
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.RefreshAuthenticationToken
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.RefreshTokenService
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.TOKEN_LIFETIME_IN_DAYS
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Duration
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("api/auth")
class AuthenticationApiController(
    private val authenticationManager: DelegatingReactiveAuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val workspaceAccessTokenService: WorkspaceAccessTokenService
) {

    @PostMapping("login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest) = GlobalScope.mono {
        val authenticationToken = UsernamePasswordAuthenticationToken(loginRequest.userName, loginRequest.password)
        val authentication = authenticationManager.authenticate(authenticationToken).awaitMono()
        val principal = authentication.principal as SecurityPrincipal
        val jwtToken = jwtService.buildJwtToken(principal)

        val response = ResponseEntity.ok()

        if (loginRequest.rememberMe) {
            response
                .withRefreshTokenCookie(
                    refreshTokenService.generateRefreshToken(principal.userName),
                    Duration.ofDays(TOKEN_LIFETIME_IN_DAYS)
                )
        }

        response.body(TokenResponse(jwtToken))
    }

    @PostMapping(path = ["login"], params = ["sharedWorkspaceToken"])
    fun login(@RequestParam("sharedWorkspaceToken") sharedWorkspaceToken: String) = GlobalScope.mono {
        val workspaceAccessToken = workspaceAccessTokenService.getValidToken(sharedWorkspaceToken)
            ?: throw BadCredentialsException("Token $sharedWorkspaceToken is not valid")
        val jwtToken = jwtService.buildJwtToken(
            createTransientUserPrincipal(workspaceAccessToken.token),
            workspaceAccessToken.validTill
        )
        TokenResponse(jwtToken)
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

        val principal = authenticatedAuth.principal as SecurityPrincipal
        if (principal.isTransient) {
            val workspaceAccessToken = workspaceAccessTokenService.getValidToken(principal.userName)
                ?: throw BadCredentialsException("Invalid workspace access token ${principal.userName}")
            TokenResponse(jwtService.buildJwtToken(principal, workspaceAccessToken.validTill))
        } else {
            TokenResponse(jwtService.buildJwtToken(principal))
        }
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
                .path("/api/auth/token")
                // todo #67: secure based on configuration
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