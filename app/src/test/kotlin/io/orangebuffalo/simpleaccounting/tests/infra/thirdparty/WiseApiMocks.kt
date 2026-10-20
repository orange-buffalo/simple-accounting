package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import com.github.tomakehurst.wiremock.client.WireMock.*

object WiseApiMocks {
    const val TOKEN = "fry-read-only-wise-token"

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
}
