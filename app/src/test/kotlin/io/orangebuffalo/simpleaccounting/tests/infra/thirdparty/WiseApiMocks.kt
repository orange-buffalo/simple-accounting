package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object WiseApiMocks {
    const val TOKEN = "fry-read-only-wise-token"

    @Volatile private var blockedStatement: BlockedStatementResponse? = null
    val blockingStatementTransformer = object : ResponseDefinitionTransformerV2 {
        override fun getName() = "block-wise-statement"
        override fun applyGlobally() = false
        override fun transform(event: ServeEvent): ResponseDefinition {
            blockedStatement?.awaitRelease()
            return event.responseDefinition
        }
    }

    fun blockStatementResponse(transactions: String = "[]"): BlockedStatementResponse {
        check(blockedStatement == null)
        val blocked = BlockedStatementResponse()
        blockedStatement = blocked
        ThirdPartyApisMocks.server.stubFor(get(urlPathEqualTo("/wise/2026Q4/profiles/101/balance-statements/301/statement.json"))
            .willReturn(okJson("""{"transactions":$transactions}""").withTransformers("block-wise-statement")))
        return blocked
    }

    class BlockedStatementResponse internal constructor() : AutoCloseable {
        private val received = CountDownLatch(1)
        private val released = CountDownLatch(1)

        internal fun awaitRelease() {
            received.countDown()
            if (!released.await(10, TimeUnit.SECONDS)) {
                released.countDown()
                error("Wise statement response was not released")
            }
        }

        fun shouldBePending() {
            received.await(10, TimeUnit.SECONDS).shouldBe(true)
            released.count.shouldBe(1L)
        }

        override fun close() {
            released.countDown()
            blockedStatement = null
        }
    }

    fun configProperties() = arrayOf("sa.integrations.wise.api-base-url=${ThirdPartyApisMocks.server.baseUrl()}/wise")

    fun setupAccounts(token: String = TOKEN) {
        stub(path = "/profiles", response = """[
            {"id":101,"type":"PERSONAL","fullName":"Philip J. Fry","extra":"ignored"},
            {"id":202,"type":"BUSINESS","fullName":"Planet Express"}
        ]""", token = token)
        stub(path = "/profiles/101/balances?types=STANDARD", response = """[
            {"id":301,"currency":"USD","type":"STANDARD","amount":{"value":1234.56}},
            {"id":302,"currency":"EUR","type":"STANDARD","amount":{"value":0}},
            {"id":303,"currency":"CAD","type":"SAVINGS","name":"Slurm fund","amount":{"value":42}}
        ]""", token = token)
        stub(path = "/profiles/202/balances?types=STANDARD", response = """[
            {"id":401,"currency":"GBP","type":"STANDARD","name":null,"amount":{"value":-12.34}}
        ]""", token = token)
    }

    fun rejectToken(status: Int = 401) {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles"))
            .willReturn(aResponse().withStatus(status)))
    }

    fun setupAccountsWithSharedIds() {
        setupAccounts()
        stub(path = "/profiles/101/balances?types=STANDARD", response = """[
            {"id":301,"currency":"USD","type":"STANDARD","amount":{"value":1234.56}},
            {"id":301,"currency":"EUR","type":"STANDARD","amount":{"value":0}}
        ]""")
    }

    fun failBalances(status: Int = 503) {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles/202/balances?types=STANDARD"))
            .willReturn(aResponse().withStatus(status)))
    }

    fun noAccounts() {
        stub(path = "/profiles", response = """[{"id":101,"fullName":"Philip J. Fry","type":"PERSONAL"}]""")
        stub(path = "/profiles/101/balances?types=STANDARD", response = "[]")
    }

    fun setupStatement(profileId: Long = 101, accountId: Long = 301, transactions: String = "[]", token: String = TOKEN) {
        ThirdPartyApisMocks.server.stubFor(get(urlPathEqualTo("/wise/2026Q4/profiles/$profileId/balance-statements/$accountId/statement.json"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .withQueryParam("type", equalTo("COMPACT"))
            .willReturn(okJson("""{"transactions":$transactions}""")))
    }

    fun failStatement(profileId: Long = 101, accountId: Long = 301, status: Int = 503) {
        ThirdPartyApisMocks.server.stubFor(get(urlPathEqualTo("/wise/2026Q4/profiles/$profileId/balance-statements/$accountId/statement.json"))
            .willReturn(aResponse().withStatus(status)))
    }

    fun removeEuroBalance(token: String = TOKEN) {
        stub(path = "/profiles/101/balances?types=STANDARD", response = """[
            {"id":301,"currency":"USD","type":"STANDARD","amount":{"value":1234.56}}
        ]""", token = token)
    }

    private fun stub(path: String, response: String, token: String = TOKEN) {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4$path"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson(response)))
    }

    fun shouldHaveNoRequests() {
        ThirdPartyApisMocks.server.verify(0, getRequestedFor(urlPathMatching("/wise/.*")))
    }

    fun shouldHaveNoStatementRequests() {
        ThirdPartyApisMocks.server.verify(0, getRequestedFor(urlPathMatching("/wise/.*/balance-statements/.*")))
    }

    fun shouldHaveStatementRequests(accountId: Long = 301, count: Int) {
        ThirdPartyApisMocks.server.verify(count, getRequestedFor(urlPathEqualTo("/wise/2026Q4/profiles/101/balance-statements/$accountId/statement.json")))
    }
}
