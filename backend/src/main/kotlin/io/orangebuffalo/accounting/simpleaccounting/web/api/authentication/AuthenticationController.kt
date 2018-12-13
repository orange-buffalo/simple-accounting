package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import io.orangebuffalo.accounting.simpleaccounting.services.security.core.DelegatingReactiveAuthenticationManager
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtService
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.RefreshAuthenticationToken
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.RefreshTokenService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
        val authentication = authenticationManager.authenticate(authenticationToken).awaitFirst()
        val userDetails = authentication.principal as UserDetails
        val jwtToken = jwtService.buildJwtToken(userDetails)
        val refreshToken: String? = if (loginRequest.rememberMe) {
            refreshTokenService.generateRefreshToken(userDetails.username)
        } else {
            null
        }

        TokensResponse(jwtToken, refreshToken)
    }

    @PostMapping("refresh-token")
    fun refreshToken(@Valid @RequestBody refreshTokenRequest: RefreshTokenRequest) = GlobalScope.mono {
        val authenticationToken = RefreshAuthenticationToken(refreshTokenRequest.refreshToken)
        val authentication = authenticationManager.authenticate(authenticationToken).awaitFirst()
        val userDetails = authentication.principal as UserDetails
        val jwtToken = jwtService.buildJwtToken(userDetails)
        TokensResponse(jwtToken, refreshTokenService.prolongToken(refreshTokenRequest.refreshToken))
    }
}

data class RefreshTokenRequest(
    @field:NotBlank val refreshToken: String
)

data class LoginRequest(
    @field:NotBlank val userName: String,
    @field:NotBlank val password: String,
    val rememberMe: Boolean = false
)

data class TokensResponse(
    val token: String,
    val refreshToken: String?
)