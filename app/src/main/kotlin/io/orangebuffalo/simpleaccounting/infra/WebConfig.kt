package io.orangebuffalo.simpleaccounting.infra

import io.orangebuffalo.simpleaccounting.business.security.SaUserRoles
import io.orangebuffalo.simpleaccounting.business.security.authentication.JwtTokenAuthenticationConverter
import io.orangebuffalo.simpleaccounting.infra.ui.SpaWebFilter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

@Configuration
@EnableWebFluxSecurity
class WebConfig : WebFluxConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:META-INF/resources/assets/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))

        registry.addResourceHandler("/favicon.ico")
            .addResourceLocations("classpath:META-INF/resources/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))

        registry.addResourceHandler("/index.html")
            .addResourceLocations("classpath:META-INF/resources/")
            .setCacheControl(CacheControl.noCache())
    }

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        @Qualifier("jwtTokenAuthenticationFilter") jwtTokenAuthenticationFilter: AuthenticationWebFilter
    ): SecurityWebFilterChain {

        return http
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { authorizeExchange ->
                authorizeExchange
                    .pathMatchers("/actuator/health").permitAll()
                    .pathMatchers("/actuator/**").denyAll()
                    .pathMatchers("/api/graphql/**").permitAll()
                    .pathMatchers("/api/auth/**").permitAll()
                    .pathMatchers("/api/downloads/**").permitAll()
                    .matchers(userActivationTokensApiControllerPublicEndpointsMatcher()).permitAll()
                    .pathMatchers("/api/users/**").hasRole(SaUserRoles.ADMIN)
                    .pathMatchers("/api/**").authenticated()
                    .pathMatchers("/**").permitAll()
            }
            .exceptionHandling { exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(bearerAuthenticationEntryPoint())
            }
            .addFilterAt(jwtTokenAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .csrf { csrf -> csrf.disable() }
            .httpBasic { httpBasic -> httpBasic.disable() }
            .formLogin { formLogin -> formLogin.disable() }
            .logout { logout -> logout.disable() }
            .build()
    }

    private fun bearerAuthenticationEntryPoint() = ServerAuthenticationEntryPoint { exchange, _ ->
        Mono.fromRunnable {
            val response = exchange.response
            response.statusCode = HttpStatus.UNAUTHORIZED
            response.headers.set("WWW-Authenticate", "Bearer")
        }
    }

    @Bean
    fun jwtTokenAuthenticationConverter(): JwtTokenAuthenticationConverter {
        return JwtTokenAuthenticationConverter()
    }

    @Bean
    fun jwtTokenAuthenticationFilter(
        authenticationManager: ReactiveAuthenticationManager,
        jwtTokenAuthenticationConverter: JwtTokenAuthenticationConverter
    ): AuthenticationWebFilter {

        return AuthenticationWebFilter(authenticationManager).apply {
            setRequiresAuthenticationMatcher(
                AndServerWebExchangeMatcher(
                    pathMatchers("/api/**"),
                    NegatedServerWebExchangeMatcher(pathMatchers("/api/auth/**"))
                )
            )
            setServerAuthenticationConverter(jwtTokenAuthenticationConverter)
            setAuthenticationFailureHandler(
                ServerAuthenticationEntryPointFailureHandler(bearerAuthenticationEntryPoint())
            )
        }
    }

    @Bean
    fun spaWebFilter() = SpaWebFilter()

}

private fun userActivationTokensApiControllerPublicEndpointsMatcher() = OrServerWebExchangeMatcher(
    // activateUser
    pathMatchers(HttpMethod.POST, "/api/user-activation-tokens/*/activate"),
    // getToken but the one for anonymous users (without a parameter)
    AndServerWebExchangeMatcher(
        pathMatchers(HttpMethod.GET, "/api/user-activation-tokens/*"),
        object : ServerWebExchangeMatcher {
            override fun matches(exchange: ServerWebExchange): Mono<ServerWebExchangeMatcher.MatchResult> {
                val match = exchange.request.queryParams["by"] == null
                return if (match) {
                    ServerWebExchangeMatcher.MatchResult.match()
                } else {
                    ServerWebExchangeMatcher.MatchResult.notMatch()
                }
            }
        }
    )
)
