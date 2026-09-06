package io.orangebuffalo.simpleaccounting.business.oauthproviders

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.tests.infra.api.stubGetRequestTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.willReturnOkJson
import io.orangebuffalo.simpleaccounting.tests.infra.api.willReturnResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@WireMockTest
class OidcProviderDiscoveryServiceTest {
    private lateinit var discoveryService: OidcProviderDiscoveryService
    private lateinit var baseUrl: String

    @BeforeEach
    fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        discoveryService = OidcProviderDiscoveryService()
        baseUrl = "http://localhost:${wireMockRuntimeInfo.httpPort}/tenant"
    }

    @Test
    fun `should discover provider configuration`() {
        stubGetRequestTo("/tenant/.well-known/openid-configuration") {
            withHeader("Accept", containing("application/json"))
            willReturnOkJson(
                /* language=json */
                """{
                    "authorization_endpoint": "http://provider.example/authorize",
                    "token_endpoint": "http://provider.example/token",
                    "userinfo_endpoint": "http://provider.example/userinfo",
                    "unsupported_value": true
                }"""
            )
        }

        val configuration = discoveryService.discover("$baseUrl/")

        configuration.authorizationUrl.shouldBe("http://provider.example/authorize")
        configuration.tokenUrl.shouldBe("http://provider.example/token")
        configuration.userInfoUrl.shouldBe("http://provider.example/userinfo")
        configuration.userIdAttribute.shouldBe("sub")
        configuration.scopes.shouldContainExactly("openid")
        verify(exactly(1), getRequestedFor(urlEqualTo("/tenant/.well-known/openid-configuration")))
    }

    @Test
    fun `should fail when discovery document is not available`() {
        stubGetRequestTo("/tenant/.well-known/openid-configuration") {
            willReturnResponse { withStatus(404) }
        }

        shouldThrow<OidcProviderDiscoveryException> {
            discoveryService.discover(baseUrl)
        }
    }

    @Test
    fun `should fail when discovery document does not provide all endpoints`() {
        stubGetRequestTo("/tenant/.well-known/openid-configuration") {
            willReturnOkJson(
                /* language=json */
                """{
                    "authorization_endpoint": "https://provider.example/authorize",
                    "token_endpoint": "https://provider.example/token"
                }"""
            )
        }

        shouldThrow<OidcProviderDiscoveryException> {
            discoveryService.discover(baseUrl)
        }
    }

    @Test
    fun `should fail when discovery document is malformed`() {
        stubGetRequestTo("/tenant/.well-known/openid-configuration") {
            willReturnOkJson("not-json")
        }

        shouldThrow<OidcProviderDiscoveryException> {
            discoveryService.discover(baseUrl)
        }
    }
}
