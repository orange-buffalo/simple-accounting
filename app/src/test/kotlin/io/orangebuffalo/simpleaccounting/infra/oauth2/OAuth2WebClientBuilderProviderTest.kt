package io.orangebuffalo.simpleaccounting.infra.oauth2

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithSaMockUser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.server.ServerWebExchange
import java.time.Duration
import java.time.Instant

@TestPropertySource(
    properties = [
        "spring.security.oauth2.client.registration.test-client.provider=test-provider",
        "spring.security.oauth2.client.registration.test-client.client-id=Client_ID",
        "spring.security.oauth2.client.registration.test-client.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.test-client.redirect-uri=http://test-host/auth-callback",
        "spring.security.oauth2.client.provider.test-provider.authorization-uri=http://test-provider.com/auth",
        "spring.security.oauth2.client.provider.test-provider.token-uri=http://localhost:\${wire-mock.port}/token"
    ]
)
@NeedsWireMock
class OAuth2WebClientBuilderProviderTest(
    @Autowired private val webClientBuilderProvider: OAuth2WebClientBuilderProvider,
    @Autowired private val jdbcAggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val transactionTemplate: TransactionTemplate,
    @WireMockPort private val wireMockPort: Int,
) : SaIntegrationTestBase() {

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should fail if client authorization does not exist`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        val exception = shouldThrow<ClientAuthorizationRequiredException> { executeResourceRequest() }
        exception.clientRegistrationId.shouldBe("test-client")
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should use access token if authorized client exists and token is not expired`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        transactionTemplate.executeWithoutResult {
            jdbcAggregateTemplate.insert(
                PersistentOAuth2AuthorizedClient(
                    clientRegistrationId = "test-client",
                    accessTokenScopes = emptySet(),
                    userName = "Fry",
                    accessToken = "accessToken",
                    accessTokenIssuedAt = Instant.now().minusMillis(100_000),
                    accessTokenExpiresAt = Instant.now().plusMillis(100_000),
                    refreshToken = null,
                    refreshTokenIssuedAt = null
                )
            )
        }
        stubGetRequestTo("/resource") {
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer accessToken"))
        }

        val response = executeResourceRequest()

        response?.statusCode().shouldBe(HttpStatus.OK)
        assertNumberOfReceivedWireMockRequests(1)
        // ensure client is not removed
        getPersistedClientsCount().shouldBe(1)
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should delete client authorization if access token is not valid and refresh token is missing`() {
// trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        transactionTemplate.executeWithoutResult {
            jdbcAggregateTemplate.insert(
                PersistentOAuth2AuthorizedClient(
                    clientRegistrationId = "test-client",
                    accessTokenScopes = emptySet(),
                    userName = "Fry",
                    accessToken = "accessToken",
                    accessTokenIssuedAt = Instant.now().minusMillis(100_000),
                    accessTokenExpiresAt = Instant.now().plusMillis(100_000),
                    refreshToken = null,
                    refreshTokenIssuedAt = null
                )
            )
        }
        stubGetRequestTo("/resource") {
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer accessToken"))
            willReturn(unauthorized())
        }

        val response = executeResourceRequest()

        response?.statusCode().shouldBe(HttpStatus.UNAUTHORIZED)
        // should remove the persisted client
        getPersistedClientsCount().shouldBe(0)
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should update access token automatically if expired when refresh token exists`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        transactionTemplate.executeWithoutResult {
            jdbcAggregateTemplate.insert(
                PersistentOAuth2AuthorizedClient(
                    clientRegistrationId = "test-client",
                    accessTokenScopes = emptySet(),
                    userName = "Fry",
                    accessToken = "accessToken",
                    accessTokenIssuedAt = Instant.now().minusMillis(100_000),
                    accessTokenExpiresAt = Instant.now().minusMillis(1),
                    refreshToken = "refreshToken",
                    refreshTokenIssuedAt = Instant.now().minusMillis(100_000)
                )
            )
        }
        stubPostRequestTo("/token") {
            withRequestBody(containing(urlEncodeParameter("grant_type" to "refresh_token")))
            withRequestBody(containing(urlEncodeParameter("client_id" to "Client_ID")))
            withRequestBody(containing(urlEncodeParameter("refresh_token" to "refreshToken")))
            willReturnOkJson(
                """{ 
                    "access_token": "newAccessToken",
                    "token_type": "bearer",
                    "expires_in": 3600
                }"""
            )
        }
        stubGetRequestTo("/resource") {
            withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer newAccessToken"))
        }

        val response = executeResourceRequest()

        response?.statusCode().shouldBe(HttpStatus.OK)
        assertNumberOfReceivedWireMockRequests(2)

        val persistedClients = jdbcAggregateTemplate.findAll(PersistentOAuth2AuthorizedClient::class.java).toList()
        persistedClients.shouldHaveSize(1)
        val persistedClient = persistedClients.first()
        persistedClient.accessToken.shouldBe("newAccessToken")
        persistedClient.accessTokenExpiresAt!!.shouldBeIn(Instant.now().plusSeconds(3580)..Instant.now().plusSeconds(3620))
    }

    private fun getPersistedClientsCount() = jdbcAggregateTemplate.count(PersistentOAuth2AuthorizedClient::class.java)

    @Suppress("DEPRECATION")
    private fun executeResourceRequest(): ClientResponse? = webClientBuilderProvider
        .forClient("test-client")
        .baseUrl("http://localhost:$wireMockPort")
        .build()
        .get()
        .uri("/resource")
        .exchange()
        .contextWrite { context ->
            // required by Spring OAuth2 support to renew the token
            context.put(ServerWebExchange::class.java, MockServerWebExchange.from(MockServerHttpRequest.get("/test")))
        }
        .block(Duration.ofSeconds(20))

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
        }
    }
}
