package io.orangebuffalo.accounting.simpleaccounting.services.security

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.PlatformUserService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Service
class JpaUserDetailsService(
        private val platformUserService: PlatformUserService
): ReactiveAuthenticationProvider {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return Mono.error(BadCredentialsException(""))
    }

    override fun supports(authenticationClass: KClass<out Any>): Boolean {
        return authenticationClass.isSubclassOf(UsernamePasswordAuthenticationToken::class)
    }

//    override fun findByUsername(username: String?): Mono<UserDetails> {
//        return platformUserService.getUserByUserName(username!!)
//                .map{ User.builder()
//                        .username(it.userName)
//                        .password(it.passwordHash)
//                        .accountExpired(false)
//                        .accountLocked(false)
//                        .credentialsExpired(false)
//                        .roles(if (it.isAdmin) "ADMIN" else "USER")
//                        .build()
//                }
//    }
}