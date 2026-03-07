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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
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
        actualUri.host.shouldBe("test-provider.com")
        actualUri.path.shouldBe("/auth")
        val params = actualUri.query.split("&").associate {
            it.substringBefore("=") to java.net.URLDecoder.decode(it.substringAfter("="), "UTF-8")
        }
        params["state"].shouldBe(savedRequest.state)
        params["redirect_uri"].shouldBe("http://test-host/auth-callback")
        params["response_type"].shouldBe("code")
        params["client_id"].shouldBe("Client_ID")
        params["scope"].shouldBe("scope2 scope1")

        savedRequest.request.scopes.shouldContainAll(listOf("scope1", "scope2"))
        savedRequest.request.clientId.shouldBe("Client_ID")
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
        actualUri.host.shouldBe("test-provider.com")
        actualUri.path.shouldBe("/auth")
        val params = actualUri.query.split("&").associate {
            it.substringBefore("=") to java.net.URLDecoder.decode(it.substringAfter("="), "UTF-8")
        }
        params.keys.shouldContain("state")
        params.keys.shouldContain("redirect_uri")
        params.keys.shouldContain("response_type")
        params.keys.shouldContain("client_id")
        params.keys.shouldContain("scope")
        params["param1"].shouldBe("value1")
    }

    @Test
    fun `should emit OAuth2FailedEvent and throw exception if error is provided in the response`() {
        mockSavedRequest(preconditions.fry)

        shouldThrow<Exception> {
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

        shouldThrow<Exception> {
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

        shouldThrow<Exception> { handleAuthorizationResponse(callbackRequestProto()) }

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
        succeededEvent.clientRegistrationId.shouldBe("test-client")
        succeededEvent.user.shouldBe(preconditions.fry)

        val persistedClients = jdbcAggregateTemplate.findAll(PersistentOAuth2AuthorizedClient::class.java).toList()
        persistedClients.shouldHaveSize(1)
        val client = persistedClients.first()
        client.accessToken.shouldBe("MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3")
        client.refreshToken.shouldBe("IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk")
        client.accessTokenScopes.shouldContainAll(listOf(ClientTokenScope("scope1"), ClientTokenScope("scope2")))
        client.accessTokenExpiresAt!!.shouldBeIn(Instant.now().plusSeconds(3580)..Instant.now().plusSeconds(3620))
        client.clientRegistrationId.shouldBe("test-client")
        client.userName.shouldBe("Fry")
    }

    private fun verifyAuthFailedEvent(fry: PlatformUser) {
        verify(authEventTestListener).onFailedAuth(capture(authFailedEventCaptor))
        val authFailedEvent = authFailedEventCaptor.value
        authFailedEvent.clientRegistrationId.shouldBe("test-client")
        authFailedEvent.user.shouldBe(fry)
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
