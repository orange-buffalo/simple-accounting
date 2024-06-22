package io.orangebuffalo.simpleaccounting.infra.api

import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.services.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.services.security.toSecurityPrincipal
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import java.time.Instant

/**
 * Configuration for the [ApiTestClient].
 */
@Configuration
class ApiTestClientConfig {

    @Bean
    fun apiTestClient(
        webTestClient: WebTestClient,
        jwtService: JwtService
    ) = ApiTestClient(webTestClient, jwtService)
}

/**
 * A client for testing API endpoints.
 * Is a wrapper around [WebTestClient] that adds meaningful semantics
 * and reduces some boilerplate.
 */
class ApiTestClient(
    private val webTestClient: WebTestClient,
    private val jwtService: JwtService,
) {

    fun getFromAnonymous() = getFrom(ANONYMOUS_USER)

    fun getFrom(actor: PlatformUser?): WebTestClient.RequestHeadersUriSpec<*> =
        if (actor == null) getFromAnonymous() else {
            val uriSpec = webTestClient.get()
            uriSpec.headers { headers -> setupAuth(headers, actor) }
            uriSpec
        }

    fun postFromAnonymous() = postFrom(ANONYMOUS_USER)

    fun postFrom(actor: PlatformUser?): WebTestClient.RequestBodyUriSpec =
        if (actor == null) postFromAnonymous() else {
            val uriSpec = webTestClient.post()
            uriSpec.headers { headers -> setupAuth(headers, actor) }
            uriSpec
        }

    private fun setupAuth(headers: HttpHeaders, actor: PlatformUser) {
        val token = jwtService.buildJwtToken(
            principal = actor.toSecurityPrincipal(),
            validTill = Instant.now().plusSeconds(Duration.ofDays(100).toSeconds())
        )
        headers.setBearerAuth(token)
    }

    companion object {
        val ANONYMOUS_USER: PlatformUser? = null
    }
}
