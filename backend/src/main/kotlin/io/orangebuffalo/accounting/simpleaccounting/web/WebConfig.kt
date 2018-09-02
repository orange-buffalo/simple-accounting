package io.orangebuffalo.accounting.simpleaccounting.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration
@EnableWebFlux
class WebConfig {
    @Bean
    fun staticResourceRouter(): RouterFunction<ServerResponse> {
        return resources("/**", ClassPathResource("META-INF/resources/"))
    }
}
