package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
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
 * Is a wrapper around [WebTestClient] that adds JWT authentication capabilities.
 */
class ApiTestClient(
    private val webTestClient: WebTestClient,
    private val jwtService: JwtService,
) {

    fun get() = webTestClient.get().also {
        it.attribute(JWT_SERVICE_ATTRIBUTE_NAME, jwtService)
    }

    fun post() = webTestClient.post().also {
        it.attribute(JWT_SERVICE_ATTRIBUTE_NAME, jwtService)
    }

    fun put() = webTestClient.put().also {
        it.attribute(JWT_SERVICE_ATTRIBUTE_NAME, jwtService)
    }
}

/**
 * A helper method to enrich a [WebTestClient.RequestHeadersSpec] with JWT authentication.
 * Important: this method should only be used for specs created from [ApiTestClient].
 */
fun <T : WebTestClient.RequestHeadersSpec<*>> T.from(platformUser: PlatformUser): T =
    this.usingPrincipal(platformUser.toSecurityPrincipal())

/**
 * A helper method to enrich a [WebTestClient.RequestHeadersSpec] with JWT authentication.
 * Important: this method should only be used for specs created from [ApiTestClient].
 */
fun <T : WebTestClient.RequestHeadersSpec<*>> T.usingSharedWorkspaceToken(workspaceToken: String): T =
    this.usingPrincipal(createTransientUserPrincipal(workspaceToken))

private fun <T : WebTestClient.RequestHeadersSpec<*>> T.usingPrincipal(principal: SecurityPrincipal): T {
   attributes {
        val jwtService = it[JWT_SERVICE_ATTRIBUTE_NAME] as JwtService?
            ?: error("This method is only allowed for specs created from ApiTestClient")
        val token = jwtService.buildJwtToken(
            principal = principal,
            validTill = Instant.now().plusSeconds(Duration.ofDays(100).toSeconds())
        )
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }
    return this
}

/**
 * A helper method to add semantics to the request spec to indicate that the request is anonymous.
 */
fun WebTestClient.RequestHeadersSpec<*>.fromAnonymous(): WebTestClient.RequestHeadersSpec<*> = headers {
    it.remove(HttpHeaders.AUTHORIZATION)
}

private const val JWT_SERVICE_ATTRIBUTE_NAME = "sa-tests.jwt-service"
