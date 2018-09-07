package io.orangebuffalo.accounting.simpleaccounting.services.security

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.PlatformUserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class SecurityConfig(
        private val platformUserService: PlatformUserService
) {

    @Bean
    fun passwordEncoder() : PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }

}