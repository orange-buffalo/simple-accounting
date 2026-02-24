package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshAuthenticationToken
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.TOKEN_LIFETIME_IN_DAYS
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Duration
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@RestController
@RequestMapping("api/auth")
class AuthenticationApi(
    private val authenticationManager: ReactiveAuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokensService: RefreshTokensService,
    private val workspaceAccessTokensService: WorkspaceAccessTokensService
) {

    @PostMapping("login")
    suspend fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<TokenResponse> {
        val authenticationToken = UsernamePasswordAuthenticationToken(loginRequest.userName, loginRequest.password)
        val authentication = authenticationManager.authenticate(authenticationToken).awaitSingle()
        val principal = authentication.principal as SecurityPrincipal
        val jwtToken = jwtService.buildJwtToken(principal)

        val response = ResponseEntity.ok()

        if (loginRequest.rememberMe) {
            response
                .withRefreshTokenCookie(
                    refreshTokensService.generateRefreshToken(principal.userName),
                    Duration.ofDays(TOKEN_LIFETIME_IN_DAYS)
                )
        }

        return response.body(TokenResponse(jwtToken))
    }

    @PostMapping("login-by-token")
    suspend fun loginBySharedWorkspaceToken(
        @RequestParam("sharedWorkspaceToken") sharedWorkspaceToken: String
    ): TokenResponse {
        val workspaceAccessToken = workspaceAccessTokensService.getValidToken(sharedWorkspaceToken)
            ?: throw BadCredentialsException("Token $sharedWorkspaceToken is not valid")
        val jwtToken = jwtService.buildJwtToken(
            createTransientUserPrincipal(workspaceAccessToken.token),
            workspaceAccessToken.validTill
        )
        return TokenResponse(jwtToken)
    }

    @PostMapping("token")
    suspend fun refreshToken(
        @CookieValue("refreshToken", required = false) refreshToken: String?,
        authentication: Authentication?
    ): TokenResponse {
        val authenticatedAuth = when {
            authentication != null && authentication.isAuthenticated -> authentication
            refreshToken != null -> {
                val authenticationToken =
                    RefreshAuthenticationToken(
                        refreshToken
                    )
                authenticationManager.authenticate(authenticationToken).awaitSingle()
            }
            else -> throw InsufficientAuthenticationException("Not authenticated")
        }

        val principal = authenticatedAuth.principal as SecurityPrincipal
        return if (principal.isTransient) {
            val workspaceAccessToken = workspaceAccessTokensService.getValidToken(principal.userName)
                ?: throw BadCredentialsException("Invalid workspace access token ${principal.userName}")
            TokenResponse(jwtService.buildJwtToken(principal, workspaceAccessToken.validTill))
        } else {
            TokenResponse(jwtService.buildJwtToken(principal))
        }
    }

    private fun ResponseEntity.BodyBuilder.withRefreshTokenCookie(
        value: String?,
        maxAge: Duration
    ): ResponseEntity.BodyBuilder = header(
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

data class LoginRequest(
    @field:NotBlank val userName: String,
    @field:NotBlank val password: String,
    val rememberMe: Boolean = false
)

data class TokenResponse(
    val token: String
)
