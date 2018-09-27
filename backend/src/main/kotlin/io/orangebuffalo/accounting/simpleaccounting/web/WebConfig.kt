package io.orangebuffalo.accounting.simpleaccounting.web

import io.orangebuffalo.accounting.simpleaccounting.web.api.authentication.JwtTokenAuthenticationConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration
@EnableWebFlux
@EnableWebFluxSecurity
class WebConfig {
    @Bean
    fun staticResourceRouter(): RouterFunction<ServerResponse> {
        return resources("/**", ClassPathResource("META-INF/resources/"))
    }

    @Bean
    fun securityWebFilterChain(
            http: ServerHttpSecurity,
            @Qualifier("jwtTokenAuthenticationFilter") jwtTokenAuthenticationFilter: AuthenticationWebFilter
    ): SecurityWebFilterChain {

        return http
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange()
                .pathMatchers("/favicon.ico").permitAll()
                .pathMatchers("/static/**").permitAll()
                .pathMatchers("/admin/**").permitAll()
                .pathMatchers("/app/**").permitAll()
                .pathMatchers("/api/login").permitAll()
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
                .pathMatchers("/api/user/**").hasRole("USER")
                .and()
                .addFilterAt(jwtTokenAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .csrf().disable()
                .httpBasic().disable()
                .formLogin().disable()
                .logout().disable()
                .build()
    }

    @Bean
    fun jwtTokenAuthenticationConverter(): JwtTokenAuthenticationConverter {
        return JwtTokenAuthenticationConverter()
    }

    @Bean
    fun jwtTokenAuthenticationFilter(
            authenticationManager: ReactiveAuthenticationManager,
            jwtTokenAuthenticationConverter: JwtTokenAuthenticationConverter): AuthenticationWebFilter {

        return AuthenticationWebFilter(authenticationManager).apply {
            setAuthenticationConverter(jwtTokenAuthenticationConverter)
        }
    }
}
