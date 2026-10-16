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
        stub(path = "/profiles/101/balances?types=STANDARD,SAVINGS", response = """[
            {"id":301,"currency":"USD","type":"STANDARD","amount":{"value":42}},
            {"id":302,"currency":"EUR","type":"SAVINGS","name":"Slurm fund"}
        ]""", token = token)
        stub(path = "/profiles/202/balances?types=STANDARD,SAVINGS", response = """[
            {"id":401,"currency":"GBP","type":"STANDARD","name":null}
        ]""", token = token)
    }

    fun rejectToken(status: Int = 401) {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles"))
            .willReturn(aResponse().withStatus(status)))
    }

    fun setupAccountsWithSharedIds() {
        setupAccounts()
        stub(path = "/profiles/101/balances?types=STANDARD,SAVINGS", response = """[
            {"id":301,"currency":"USD","type":"STANDARD"},
            {"id":301,"currency":"EUR","type":"STANDARD"}
        ]""")
    }

    fun failBalances(status: Int = 503) {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles/202/balances?types=STANDARD,SAVINGS"))
            .willReturn(aResponse().withStatus(status)))
    }

    fun noAccounts() {
        stub(path = "/profiles", response = """[{"id":101,"fullName":"Philip J. Fry"}]""")
        stub(path = "/profiles/101/balances?types=STANDARD,SAVINGS", response = "[]")
    }

    fun removeJar(token: String = TOKEN) {
        stub(path = "/profiles/101/balances?types=STANDARD,SAVINGS", response = """[
            {"id":301,"currency":"USD","type":"STANDARD"}
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
