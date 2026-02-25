package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshAuthenticationToken
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("api/auth")
class AuthenticationApi(
    private val authenticationManager: ReactiveAuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokensService: RefreshTokensService,
    private val workspaceAccessTokensService: WorkspaceAccessTokensService
) {

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
}

data class TokenResponse(
    val token: String
)
