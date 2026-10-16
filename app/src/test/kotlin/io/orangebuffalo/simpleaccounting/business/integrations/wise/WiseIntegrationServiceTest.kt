package io.orangebuffalo.simpleaccounting.business.integrations.wise

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.junit.jupiter.api.Test
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.http.Fault
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.slf4j.LoggerFactory

@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
@TestExecutionListeners(listeners = [ThirdPartyApisMocksListener::class], mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class WiseIntegrationServiceTest(@Autowired private val service: WiseIntegrationService) : SaIntegrationTestBase() {
    private val token = WiseApiMocks.TOKEN
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    private fun verifyToken() = runAs(preconditions.fry.toSecurityPrincipal()) {
        service.verifyToken(workspaceId = preconditions.workspace.id!!, token = token)
    }

    @Test
    fun `should preserve long identifiers and ignore additional provider fields`() {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson("""[{"id":9007199254740993,"fullName":"Bender","type":"PERSONAL"}]""")))
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles/9007199254740993/balances?types=STANDARD,SAVINGS"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson("""[{"id":9007199254740995,"currency":"USD","type":"STANDARD","amount":{"value":42}}]""")))

        verifyToken().shouldBe(listOf(
            WiseAccount(9007199254740993, "Bender", 9007199254740995, "USD", null, "STANDARD"),
        ))
    }

    @Test
    fun `should sanitize rejected token and upstream error bodies`() {
        for (status in listOf(401, 403, 429, 503)) {
            ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles")).willReturn(aResponse().withStatus(status).withBody(token)))
            val exception = if (status == 401 || status == 403) {
                shouldThrow<WiseInvalidTokenException> { verifyToken() }
                    .also { it.message.shouldBe("Wise rejected the token") }
            } else {
                shouldThrow<WiseUnavailableException> { verifyToken() }
                    .also { it.message.shouldBe("Wise accounts could not be loaded") }
            }
            exception.cause.shouldBe(null)
        }
    }

    @Test
    fun `should treat malformed responses as unavailable without retaining the payload`() {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles")).willReturn(okJson("""{"unexpected":"$token"}""")))
        val exception = shouldThrow<WiseUnavailableException> { verifyToken() }
        exception.message.shouldBe("Wise accounts could not be loaded")
        exception.cause.shouldBe(null)
    }

    @Test
    fun `should log actionable diagnostics without exposing credentials`() {
        val logger = LoggerFactory.getLogger(WiseApiClient::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            val responses = listOf(
                aResponse().withStatus(404).withBody("<html>Missing Wise endpoint $token</html>") to
                    listOf("HTTP 404", "<html>Missing Wise endpoint [REDACTED]</html>"),
                aResponse().withStatus(503).withBody("Wise unavailable") to listOf("HTTP 503", "Wise unavailable"),
                aResponse().withStatus(204) to listOf("HTTP 204", "empty response body"),
                okJson("""{"unexpected":"$token"}""") to listOf("HTTP 200", "invalid response body", "[REDACTED]"),
                aResponse().withFault(Fault.EMPTY_RESPONSE) to listOf("No usable response received", "ResourceAccessException"),
            )
            for ((response, expected) in responses) {
                appender.list.clear()
                ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles")).willReturn(response))
                shouldThrow<WiseUnavailableException> { verifyToken() }
                val message = appender.list.single().formattedMessage
                message.shouldContain("Wise GET http://")
                message.shouldContain("/wise/2026Q4/profiles failed:")
                expected.forEach { message.shouldContain(it) }
                message.shouldNotContain(token)
                (message.length <= 4096).shouldBe(true)
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
