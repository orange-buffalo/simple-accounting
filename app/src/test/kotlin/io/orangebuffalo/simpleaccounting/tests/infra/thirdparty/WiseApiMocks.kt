package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import com.github.tomakehurst.wiremock.client.WireMock.*

object WiseApiMocks {
    const val TOKEN = "fry-read-only-wise-token"

    fun configProperties() = arrayOf("sa.wise.api-base-url=${ThirdPartyApisMocks.server.baseUrl()}/wise")

    fun accounts() {
        stub("/profiles", """[
            {"id":101,"type":"PERSONAL","fullName":"Philip J. Fry","extra":"ignored"},
            {"id":202,"type":"BUSINESS","fullName":"Planet Express"}
        ]""")
        stub("/profiles/101/balances?types=STANDARD,SAVINGS", """[
            {"id":301,"currency":"USD","type":"STANDARD","amount":{"value":42}},
            {"id":302,"currency":"EUR","type":"SAVINGS","name":"Slurm fund"}
        ]""")
        stub("/profiles/202/balances?types=STANDARD,SAVINGS", """[
            {"id":401,"currency":"GBP","type":"STANDARD","name":null}
        ]""")
    }

    fun rejectToken(status: Int = 401) {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles"))
            .willReturn(aResponse().withStatus(status)))
    }

    fun failBalances(status: Int = 503) {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles/202/balances?types=STANDARD,SAVINGS"))
            .willReturn(aResponse().withStatus(status)))
    }

    fun noAccounts() {
        stub("/profiles", """[{"id":101,"fullName":"Philip J. Fry"}]""")
        stub("/profiles/101/balances?types=STANDARD,SAVINGS", "[]")
    }

    private fun stub(path: String, response: String) {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4$path"))
            .withHeader("Authorization", equalTo("Bearer $TOKEN"))
            .willReturn(okJson(response)))
    }

    fun shouldHaveNoRequests() {
        ThirdPartyApisMocks.server.verify(0, getRequestedFor(urlPathMatching("/wise/.*")))
    }
}
