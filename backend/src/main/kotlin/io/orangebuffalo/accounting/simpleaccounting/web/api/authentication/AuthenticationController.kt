package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import io.orangebuffalo.accounting.simpleaccounting.services.security.AuthenticationManager
import io.orangebuffalo.accounting.simpleaccounting.services.security.JwtService
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import kotlin.reflect.full.cast

@RestController
@RequestMapping("api")
class AuthenticationController(
        private val authenticationManager: AuthenticationManager,
        private val jwtService: JwtService
) {

    @PostMapping("login", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun login(@RequestBody loginRequest: Mono<LoginRequest>): Mono<LoginResponse> {
        return loginRequest
                .map { UsernamePasswordAuthenticationToken(it.userName, it.password) }
                .flatMap(authenticationManager::authenticate)
                .map(Authentication::getPrincipal)
                .map(UserDetails::class::cast)
                .map(jwtService::buildJwtToken)
                .map(::LoginResponse)
    }

}