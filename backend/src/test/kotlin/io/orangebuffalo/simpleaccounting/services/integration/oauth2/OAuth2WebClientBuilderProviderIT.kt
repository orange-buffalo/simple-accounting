package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.WithSaMockUser
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.utils.NeedsWireMock
import io.orangebuffalo.simpleaccounting.utils.WireMockPort
import io.orangebuffalo.simpleaccounting.utils.assertNumberOfStubbedRequests
import io.orangebuffalo.simpleaccounting.utils.urlEncodeParameter
import org.assertj.core.api.Assertions.*
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
import java.time.temporal.ChronoUnit.SECONDS

@SimpleAccountingIntegrationTest
@TestPropertySource(
    properties = [
        "spring.security.oauth2.client.registration.test-client.provider=test-provider",
        "spring.security.oauth2.client.registration.test-client.client-id=Client ID",
        "spring.security.oauth2.client.registration.test-client.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.test-client.redirect-uri=http://test-host/auth-callback",
        "spring.security.oauth2.client.provider.test-provider.authorization-uri=http://test-provider.com/auth",
        "spring.security.oauth2.client.provider.test-provider.token-uri=http://localhost:\${wire-mock.port}/token"
    ]
)
@NeedsWireMock
class OAuth2WebClientBuilderProviderIT(
    @Autowired private val webClientBuilderProvider: OAuth2WebClientBuilderProvider,
    @Autowired private val jdbcAggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val transactionTemplate: TransactionTemplate,
    @WireMockPort private val wireMockPort: Int
) {

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should fail if client authorization does not exist`(testData: WebClientProviderTestData) {
        assertThatThrownBy { executeResourceRequest() }
            .isInstanceOfSatisfying(ClientAuthorizationRequiredException::class.java) { exception ->
                assertThat(exception.clientRegistrationId).isEqualTo("test-client")
            }
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should use access token if authorized client exists and token is not expired`(testData: WebClientProviderTestData) {
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
        stubFor(
            get(urlPathEqualTo("/resource"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer accessToken"))
        )

        val response = executeResourceRequest()

        assertThat(response?.statusCode()).isEqualTo(HttpStatus.OK)
        assertNumberOfStubbedRequests(1)
        // ensure client is not removed
        assertThat(getPersistedClientsCount()).isOne()
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should delete client authorization if access token is not valid and refresh token is missing`(testData: WebClientProviderTestData) {
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
        stubFor(
            get(urlPathEqualTo("/resource"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer accessToken"))
                .willReturn(unauthorized())
        )

        val response = executeResourceRequest()

        assertThat(response?.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertNumberOfStubbedRequests(1)
        // should remove the persisted client
        assertThat(getPersistedClientsCount()).isZero()
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should update access token automatically if expired when refresh token exists`(testData: WebClientProviderTestData) {
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
        stubFor(
            post(urlPathEqualTo("/token"))
                .withRequestBody(containing(urlEncodeParameter("grant_type" to "refresh_token")))
                .withRequestBody(containing(urlEncodeParameter("client_id" to "Client ID")))
                .withRequestBody(containing(urlEncodeParameter("refresh_token" to "refreshToken")))
                .willReturn(
                    okJson(
                        """{ 
                            "access_token": "newAccessToken",
                            "token_type": "bearer",
                            "expires_in": 3600
                        }"""
                    )
                )
        )
        stubFor(
            get(urlPathEqualTo("/resource"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer newAccessToken"))

        )

        val response = executeResourceRequest()

        assertThat(response?.statusCode()).isEqualTo(HttpStatus.OK)
        assertNumberOfStubbedRequests(2)

        val persistedClients = jdbcAggregateTemplate.findAll(PersistentOAuth2AuthorizedClient::class.java)
        assertThat(persistedClients).hasOnlyOneElementSatisfying { persistedClient ->
            assertThat(persistedClient.accessToken).isEqualTo("newAccessToken")
            assertThat(persistedClient.accessTokenExpiresAt)
                .isCloseTo(Instant.now().plusSeconds(3600), within(20, SECONDS))
        }
    }

    private fun getPersistedClientsCount() = jdbcAggregateTemplate.count(PersistentOAuth2AuthorizedClient::class.java)

    private fun executeResourceRequest(): ClientResponse? = webClientBuilderProvider
        .forClient("test-client")
        .baseUrl("http://localhost:$wireMockPort")
        .build()
        .get()
        .uri("/resource")
        .exchange()
        .subscriberContext { context ->
            // required by Spring OAuth2 support to renew the token
            context.put(ServerWebExchange::class.java, MockServerWebExchange.from(MockServerHttpRequest.get("/test")))
        }
        .block(Duration.ofSeconds(20))

    class WebClientProviderTestData : TestData {
        val fry = Prototypes.fry()
        override fun generateData() = listOf(fry)
    }
}
