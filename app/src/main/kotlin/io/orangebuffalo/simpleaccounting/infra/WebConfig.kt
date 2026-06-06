package io.orangebuffalo.simpleaccounting.infra

import io.orangebuffalo.simpleaccounting.infra.ui.SpaWebFilter
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
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
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { authorizeExchange ->
                authorizeExchange
                    .matchers(EndpointRequest.to(HealthEndpoint::class.java)).permitAll()
                    .matchers(EndpointRequest.toAnyEndpoint()).denyAll()
                    .pathMatchers("/api/graphql/**").permitAll()
                    .pathMatchers("/api/documents/**").permitAll()
                    .pathMatchers("/**").permitAll()
            }
            .csrf { csrf -> csrf.disable() }
            .httpBasic { httpBasic -> httpBasic.disable() }
            .formLogin { formLogin -> formLogin.disable() }
            .logout { logout -> logout.disable() }
            .build()
    }

    @Bean
    fun spaWebFilter() = SpaWebFilter()

}
