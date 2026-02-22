package io.orangebuffalo.simpleaccounting.infra.oauth2

import com.github.tomakehurst.wiremock.client.WireMock.badRequest
import com.github.tomakehurst.wiremock.client.WireMock.containing
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.ClientTokenScope
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.NeedsWireMock
import io.orangebuffalo.simpleaccounting.tests.infra.api.stubPostRequestTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.urlEncodeParameter
import io.orangebuffalo.simpleaccounting.tests.infra.api.willReturnOkJson
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithSaMockUser
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS
import java.util.function.Consumer

@TestPropertySource(
    properties = [
        "spring.security.oauth2.client.registration.test-client.provider=test-provider",
        "spring.security.oauth2.client.registration.test-client.client-id=Client_ID",
        "spring.security.oauth2.client.registration.test-client.client-secret=Client_Secret",
        "spring.security.oauth2.client.registration.test-client.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.test-client.redirect-uri=http://test-host/auth-callback",
        "spring.security.oauth2.client.registration.test-client.scope=scope2,scope1",
        "spring.security.oauth2.client.provider.test-provider.authorization-uri=http://test-provider.com/auth",
        "spring.security.oauth2.client.provider.test-provider.token-uri=http://localhost:\${wire-mock.port}/token"
    ]
)
@NeedsWireMock
internal class OAuth2ClientAuthorizationProviderTest(
    @Autowired private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider,
    @Autowired private val jdbcAggregateTemplate: JdbcAggregateTemplate,
) : SaIntegrationTestBase() {

    @MockitoBean
    lateinit var authEventTestListener: AuthEventTestListener

    @MockitoBean
    lateinit var savedAuthorizationRequestRepository: SavedAuthorizationRequestRepository

    @Captor
    lateinit var savedRequestCaptor: ArgumentCaptor<SavedAuthorizationRequest>

    @Captor
    lateinit var authFailedEventCaptor: ArgumentCaptor<OAuth2FailedEvent>

    @Captor
    lateinit var authSucceededEventCaptor: ArgumentCaptor<OAuth2SucceededEvent>

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should create a valid authorization URL`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        val actualUrl = runBlocking { clientAuthorizationProvider.buildAuthorizationUrl("test-client") }

        verifyBlocking(savedAuthorizationRequestRepository) { save(capture(savedRequestCaptor)) }

        val actualUri = URI(actualUrl)
        val savedRequest = savedRequestCaptor.value
        assertThat(actualUri)
            .hasAuthority("test-provider.com")
            .hasPath("/auth")
            .hasParameter("state", savedRequest.state)
            .hasParameter("redirect_uri", "http://test-host/auth-callback")
            .hasParameter("response_type", "code")
            .hasParameter("client_id", "Client_ID")
            .hasParameter("scope", "scope2 scope1")

        assertThat(savedRequest.request.scopes).contains("scope1", "scope2")
        assertThat(savedRequest.request.clientId).isEqualTo("Client_ID")
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should add additional parameters to authorization URL`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        val actualUrl = runBlocking {
            clientAuthorizationProvider.buildAuthorizationUrl("test-client", mapOf("param1" to "value1"))
        }

        val actualUri = URI(actualUrl)
        assertThat(actualUri)
            .hasAuthority("test-provider.com")
            .hasPath("/auth")
            .hasParameter("state")
            .hasParameter("redirect_uri")
            .hasParameter("response_type")
            .hasParameter("client_id")
            .hasParameter("scope")
            .hasParameter("param1", "value1")
    }

    @Test
    fun `should emit OAuth2FailedEvent and throw exception if error is provided in the response`() {
        mockSavedRequest(preconditions.fry)

        assertThatThrownBy {
            handleAuthorizationResponse(
                callbackRequestProto().copy(
                    code = null,
                    error = "error"
                )
            )
        }

        verifyAuthFailedEvent(preconditions.fry)
    }

    @Test
    fun `should emit OAuth2FailedEvent and throw exception if code is not provided in the response`() {
        mockSavedRequest(preconditions.fry)

        assertThatThrownBy {
            handleAuthorizationResponse(
                callbackRequestProto().copy(
                    code = null,
                    error = null
                )
            )
        }

        verifyAuthFailedEvent(preconditions.fry)
    }

    @Test
    fun `should emit OAuth2FailedEvent and throw exception if token endpoint fails`() {
        mockSavedRequest(preconditions.fry)
        stubPostRequestTo("/token") {
            willReturn(badRequest().withBody("""{ "error": "some bad request" }"""))
        }

        assertThatThrownBy { handleAuthorizationResponse(callbackRequestProto()) }

        verifyAuthFailedEvent(preconditions.fry)
    }

    @Test
    fun `should call token endpoint with proper parameters`() {
        mockSavedRequest(preconditions.fry)

        stubPostRequestTo("/token") {
            withRequestBody(containing(urlEncodeParameter("grant_type" to "authorization_code")))
            withRequestBody(containing(urlEncodeParameter("code" to "testCode")))
            withRequestBody(containing(urlEncodeParameter("redirect_uri" to "http://test-host/auth-callback")))
            withBasicAuth("Client_ID", "Client_Secret")
            willReturnOkJson(
                """{ 
                    "access_token": "access_token",
                    "token_type": "bearer",
                    "expires_in": 3600,
                    "refresh_token": "refresh_token",
                    "scope": "scope1 scope2"
                }"""
            )
        }

        handleAuthorizationResponse(callbackRequestProto())
    }

    @Test
    fun `should save persisted client and emit successful auth even on token response`() {
        mockSavedRequest(preconditions.fry)

        stubPostRequestTo("/token") {
            willReturnOkJson(
                """{ 
                    "access_token": "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
                    "token_type": "bearer",
                    "expires_in": 3600,
                    "refresh_token": "IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk",
                    "scope": "scope1 scope2"
                }"""
            )
        }

        handleAuthorizationResponse(callbackRequestProto())

        verify(authEventTestListener).onSucceededAuth(capture(authSucceededEventCaptor))
        val succeededEvent = authSucceededEventCaptor.value
        assertThat(succeededEvent.clientRegistrationId).isEqualTo("test-client")
        assertThat(succeededEvent.user).isEqualTo(preconditions.fry)

        val persistedClients = jdbcAggregateTemplate.findAll(PersistentOAuth2AuthorizedClient::class.java)
        assertThat(persistedClients).singleElement().satisfies(Consumer { client ->
            assertThat(client.accessToken).isEqualTo("MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3")
            assertThat(client.refreshToken).isEqualTo("IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk")
            assertThat(client.accessTokenScopes).contains(ClientTokenScope("scope1"), ClientTokenScope("scope2"))
            assertThat(client.accessTokenExpiresAt).isCloseTo(Instant.now().plusSeconds(3600), within(20, SECONDS))
            assertThat(client.clientRegistrationId).isEqualTo("test-client")
            assertThat(client.userName).isEqualTo("Fry")
        })
    }

    private fun verifyAuthFailedEvent(fry: PlatformUser) {
        verify(authEventTestListener).onFailedAuth(capture(authFailedEventCaptor))
        val authFailedEvent = authFailedEventCaptor.value
        assertThat(authFailedEvent.clientRegistrationId).isEqualTo("test-client")
        assertThat(authFailedEvent.user).isEqualTo(fry)
    }

    private fun callbackRequestProto() = OAuth2AuthorizationCallbackRequest(
        code = "testCode",
        error = null,
        state = "testState"
    )

    private fun mockSavedRequest(owner: PlatformUser) {
        savedAuthorizationRequestRepository.stub {
            onBlocking { findByStateAndRemove("testState") } doReturn SavedAuthorizationRequest(
                owner = owner,
                clientRegistrationId = "test-client",
                state = "testState",
                request = OAuth2AuthorizationRequest.authorizationCode()
                    .clientId("Client_ID")
                    .authorizationUri("http://test-provider.com/auth")
                    .redirectUri("http://test-host/auth-callback")
                    .scopes(setOf("scope1", "scope2"))
                    .build()
            )
        }
    }

    private fun handleAuthorizationResponse(request: OAuth2AuthorizationCallbackRequest) {
        runBlocking { clientAuthorizationProvider.handleAuthorizationResponse(request) }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
        }
    }

    // workaround for https://github.com/spring-projects/spring-framework/issues/18907
    interface AuthEventTestListener {
        @EventListener
        fun onFailedAuth(event: OAuth2FailedEvent)

        @EventListener
        fun onSucceededAuth(event: OAuth2SucceededEvent)
    }
}
