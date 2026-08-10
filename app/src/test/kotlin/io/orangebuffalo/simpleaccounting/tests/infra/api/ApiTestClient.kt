package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.client.RestTestClient
import java.time.Duration
import java.time.Instant

/**
 * Configuration for the [ApiTestClient].
 */
@Configuration
class ApiTestClientConfig {

    @Bean
    fun apiTestClient(
        restTestClient: RestTestClient,
        jwtService: JwtService
    ) = ApiTestClient(restTestClient, jwtService)
}

/**
 * A client for testing API endpoints.
 * Is a wrapper around [RestTestClient] that adds JWT authentication capabilities.
 */
class ApiTestClient(
    private val restTestClient: RestTestClient,
    private val jwtService: JwtService,
) {

    fun get(): RestTestClient.RequestHeadersUriSpec<*> = restTestClient.get().also {
        it.attribute(JWT_SERVICE_ATTRIBUTE_NAME, jwtService)
    }

    fun post(): RestTestClient.RequestBodyUriSpec = restTestClient.post().also {
        it.attribute(JWT_SERVICE_ATTRIBUTE_NAME, jwtService)
    }

    fun put(): RestTestClient.RequestBodyUriSpec = restTestClient.put().also {
        it.attribute(JWT_SERVICE_ATTRIBUTE_NAME, jwtService)
    }
}

/**
 * A helper method to enrich a [RestTestClient.RequestHeadersSpec] with JWT authentication.
 * Important: this method should only be used for specs created from [ApiTestClient].
 */
fun <T : RestTestClient.RequestHeadersSpec<*>> T.from(platformUser: PlatformUser): T =
    this.usingPrincipal(platformUser.toSecurityPrincipal())

/**
 * A helper method to enrich a [RestTestClient.RequestHeadersSpec] with JWT authentication.
 * Important: this method should only be used for specs created from [ApiTestClient].
 */
fun <T : RestTestClient.RequestHeadersSpec<*>> T.usingSharedWorkspaceToken(workspaceToken: String): T =
    this.usingPrincipal(createTransientUserPrincipal(workspaceToken))

private fun <T : RestTestClient.RequestHeadersSpec<*>> T.usingPrincipal(principal: SecurityPrincipal): T {
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
fun <T : RestTestClient.RequestHeadersSpec<*>> T.fromAnonymous(): T = apply {
    headers { it.remove(HttpHeaders.AUTHORIZATION) }
}

private const val JWT_SERVICE_ATTRIBUTE_NAME = "sa-tests.jwt-service"
