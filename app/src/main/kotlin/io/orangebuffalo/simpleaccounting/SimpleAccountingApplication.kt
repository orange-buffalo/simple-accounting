package io.orangebuffalo.simpleaccounting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientWebSecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        // JOOQ brings dependencies that activate this autoconfig,
        // there is no way to disable it via properties
        R2dbcAutoConfiguration::class,
        // This auto-configuration creates a SecurityWebFilterChain with anyExchange().authenticated()
        // and oauth2Login(), which conflicts with our custom security chain that permits anonymous access
        // to download endpoints. We only use OAuth2 for Google Drive integration, not for user authentication.
        ReactiveOAuth2ClientWebSecurityAutoConfiguration::class,
    ]
)
class SimpleAccountingApplication

fun main(args: Array<String>) {
    runApplication<SimpleAccountingApplication>(*args)
}
