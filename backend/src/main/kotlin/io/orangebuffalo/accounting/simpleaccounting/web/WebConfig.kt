package io.orangebuffalo.accounting.simpleaccounting.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
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
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
                .authorizeExchange()
                .pathMatchers("/favicon.ico").permitAll()
                .pathMatchers("/static/**").permitAll()
                .pathMatchers("/admin/**").permitAll()
                .pathMatchers("/app/**").permitAll()
                .pathMatchers("/api/login").permitAll()
                .and()
                .csrf().disable()
                .build()
    }

}
