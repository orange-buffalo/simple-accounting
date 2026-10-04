package io.orangebuffalo.simpleaccounting.infra.thirdparty.wise

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@WireMockTest
class WiseApiClientTest {
    private lateinit var client: WiseApiClient
    private val token = "bender-personal-token"

    @BeforeEach
    fun setup(runtime: WireMockRuntimeInfo) {
        client = WiseApiClient(WiseProperties(apiBaseUrl = runtime.httpBaseUrl))
    }

    @Test
    fun `should preserve long identifiers and ignore additional provider fields`() {
        stubFor(get(urlEqualTo("/2026Q4/profiles"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson("""[{"id":9007199254740993,"fullName":"Bender","type":"PERSONAL"}]""")))
        stubFor(get(urlEqualTo("/2026Q4/profiles/9007199254740993/balances?types=STANDARD,SAVINGS"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson("""[{"id":9007199254740995,"currency":"USD","type":"STANDARD","amount":{"value":42}}]""")))

        client.getAccounts(token).shouldBe(listOf(
            WiseAccount(9007199254740993, "Bender", 9007199254740995, "USD", null, "STANDARD"),
        ))
    }

    @Test
    fun `should sanitize rejected token and upstream error bodies`() {
        for (status in listOf(401, 403, 429, 503)) {
            stubFor(get(urlEqualTo("/2026Q4/profiles")).willReturn(aResponse().withStatus(status).withBody(token)))
            val exception = if (status == 401 || status == 403) {
                shouldThrow<WiseInvalidTokenException> { client.getAccounts(token) }
                    .also { it.message.shouldBe("Wise rejected the token") }
            } else {
                shouldThrow<WiseUnavailableException> { client.getAccounts(token) }
                    .also { it.message.shouldBe("Wise accounts could not be loaded") }
            }
            exception.cause.shouldBe(null)
        }
    }

    @Test
    fun `should treat malformed responses as unavailable without retaining the payload`() {
        stubFor(get(urlEqualTo("/2026Q4/profiles")).willReturn(okJson("""{"unexpected":"$token"}""")))
        val exception = shouldThrow<WiseUnavailableException> { client.getAccounts(token) }
        exception.message.shouldBe("Wise accounts could not be loaded")
        exception.cause.shouldBe(null)
    }
}
