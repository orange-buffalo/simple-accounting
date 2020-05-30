package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.WithSaMockUser
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.utils.getFormParameters
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.server.ServerWebExchange
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

private val mockWebServerPort = SocketUtils.findAvailableTcpPort()

@SimpleAccountingIntegrationTest
@TestPropertySource(
    properties = [
        "spring.security.oauth2.client.registration.test-client.provider=test-provider",
        "spring.security.oauth2.client.registration.test-client.client-id=Client ID",
        "spring.security.oauth2.client.registration.test-client.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.test-client.redirect-uri=http://test-host/auth-callback",
        "spring.security.oauth2.client.provider.test-provider.authorization-uri=http://test-provider.com/auth"
    ]
)
class OAuth2WebClientBuilderProviderIT(
    @Autowired private val webClientBuilderProvider: OAuth2WebClientBuilderProvider,
    @Autowired private val jdbcAggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val transactionTemplate: TransactionTemplate
) {

    private lateinit var mockWebServer: MockWebServer

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun setupContextProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.security.oauth2.client.provider.test-provider.token-uri") {
                "http://localhost:${mockWebServerPort}/token"
            }
        }
    }

    @BeforeEach
    fun initWebServer() {
        mockWebServer = MockWebServer()
        mockWebServer.start(mockWebServerPort)
        jdbcAggregateTemplate.deleteAll(PersistentOAuth2AuthorizedClient::class.java)
    }

    @AfterEach
    fun shutdownWebServer() {
        mockWebServer.shutdown()
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should fail if client authorization does not exist`(testData: WebClientProviderTestData) {
        mockWebServer.enqueue(MockResponse())

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

        mockWebServer.enqueue(MockResponse())

        val response = executeResourceRequest()
        assertThat(response?.statusCode()).isEqualTo(HttpStatus.OK)

        assertThat(mockWebServer.requestCount).isOne()
        val actualRequest = mockWebServer.takeRequest()
        assertThat(actualRequest.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer accessToken")

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

        mockWebServer.enqueue(MockResponse().setResponseCode(HttpStatus.UNAUTHORIZED.value()))

        val response = executeResourceRequest()
        assertThat(response?.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED)

        assertThat(mockWebServer.requestCount).isOne()
        val actualRequest = mockWebServer.takeRequest()
        assertThat(actualRequest.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer accessToken")

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

        mockWebServer.enqueue(
            MockResponse()
                .setBody(
                    """{ 
                        "access_token": "newAccessToken",
                        "token_type": "bearer",
                        "expires_in": 3600
                    }"""
                )
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        )
        mockWebServer.enqueue(MockResponse())

        val response = executeResourceRequest()
        assertThat(response?.statusCode()).isEqualTo(HttpStatus.OK)

        assertThat(mockWebServer.requestCount).isEqualTo(2)

        val accessTokenRequest = mockWebServer.takeRequest()
        assertThat(accessTokenRequest.method).isEqualTo(HttpMethod.POST.name)
        assertThat(accessTokenRequest.path).isEqualTo("/token")
        assertThat(getFormParameters(accessTokenRequest)).contains(
            "grant_type" to "refresh_token",
            "client_id" to "Client ID",
            "refresh_token" to "refreshToken"
        )

        val resourceRequest = mockWebServer.takeRequest()
        assertThat(resourceRequest.path).isEqualTo("/resource")
        assertThat(resourceRequest.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer newAccessToken")

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
        .baseUrl("http://localhost:$mockWebServerPort")
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
