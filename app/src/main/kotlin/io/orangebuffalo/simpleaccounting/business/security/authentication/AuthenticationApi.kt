package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/auth")
class AuthenticationApi(
    private val jwtService: JwtService,
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
}

data class TokenResponse(
    val token: String
)
