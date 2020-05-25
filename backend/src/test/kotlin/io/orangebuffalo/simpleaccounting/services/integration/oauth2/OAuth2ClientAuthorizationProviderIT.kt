package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import com.nhaarman.mockitokotlin2.capture
import com.nhaarman.mockitokotlin2.verifyBlocking
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.WithSaMockUser
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import java.net.URI

@SimpleAccountingIntegrationTest
@TestPropertySource(
    properties = [
        "spring.security.oauth2.client.registration.test-client.provider=test-provider",
        "spring.security.oauth2.client.registration.test-client.client-id=Client ID",
        "spring.security.oauth2.client.registration.test-client.client-secret=Client Secret",
        "spring.security.oauth2.client.registration.test-client.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.test-client.redirect-uri=http://test-host/auth-callback",
        "spring.security.oauth2.client.registration.test-client.scope=scope2,scope1",
        "spring.security.oauth2.client.provider.test-provider.authorization-uri=http://test-provider.com/auth",
        "spring.security.oauth2.client.provider.test-provider.token-uri=http://test-provider.com/token"

    ]
)
internal class OAuth2ClientAuthorizationProviderIT(
    @Autowired private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider
) {

    @MockBean
    lateinit var savedAuthorizationRequestRepository: SavedAuthorizationRequestRepository

    @Captor
    lateinit var savedRequestCaptor: ArgumentCaptor<SavedAuthorizationRequest>

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should create a valid authorization URL`(testData: OAuth2ClientAuthorizationProviderTestData) {
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
            .hasParameter("client_id", "Client ID")
            .hasParameter("scope", "scope2 scope1")

        assertThat(savedRequest.request.scopes).contains("scope1", "scope2")
        assertThat(savedRequest.request.clientId).isEqualTo("Client ID")
    }

    @Test
    @WithSaMockUser(userName = "Fry")
    fun `should add additional parameters to authorization URL`(testData: OAuth2ClientAuthorizationProviderTestData) {
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

    class OAuth2ClientAuthorizationProviderTestData : TestData {
        override fun generateData() = listOf(Prototypes.fry())
    }
}
