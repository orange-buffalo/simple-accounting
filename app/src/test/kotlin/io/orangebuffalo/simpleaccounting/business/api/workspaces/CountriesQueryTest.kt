package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

class CountriesQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {
    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["AUD"])
        fun `should require authentication`(currency: String?) {
            client.graphql { countries(currency = currency) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = "countries")
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["AUD"])
        fun `should reject admin users`(currency: String?) {
            val farnsworth = preconditions { farnsworth() }
            client.graphql { countries(currency = currency) }
                .from(farnsworth)
                .executeAndVerifyNotAuthorized(path = "countries")
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["AUD"])
        fun `should reject workspace access tokens`(currency: String?) {
            val token = preconditions {
                workspaceAccessToken(validTill = MOCK_TIME.plusSeconds(10000))
            }
            client.graphql { countries(currency = currency) }
                .usingSharedWorkspaceToken(token.token)
                .executeAndVerifyNotAuthorized(path = "countries")
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return all ISO countries for workspace setup without an existing workspace`() {
            val fry = preconditions { fry() }
            val expectedCountries = """
                AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW
                BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI
                FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM
                IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC
                MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM
                PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO
                SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI
                VN VU WF WS YE YT ZA ZM ZW
            """.trim().split(Regex("\\s+"))
            client.graphql { countries(currency = null) }
                .from(fry)
                .executeAndVerifyResponse("countries" to buildJsonArray {
                    expectedCountries.forEach { add(it) }
                })
        }

        @ParameterizedTest
        @CsvSource(
            delimiter = '|',
            value = [
                "AUD | AU CC CX HM KI NF NR TV",
                "USD | AS BQ EC FM GU HT IO MH MP PA PR PW TC TL UM US VG VI",
                "EUR | AD AT AX BE BG BL CY DE EE ES FI FR GF GP GR HR IE IT LT LU LV MC ME MF MQ MT NL PL PM PT RE SI SK SM TF VA YT",
                "INR | BT IN",
                "ZAR | LS NA ZA",
            ],
        )
        fun `should restrict countries by supported currency`(currency: String, expectedCountries: String) {
            val fry = preconditions { fry() }
            client.graphql { countries(currency = currency) }
                .from(fry)
                .executeAndVerifyResponse("countries" to buildJsonArray {
                    expectedCountries.split(" ").forEach { add(it) }
                })
        }

        @ParameterizedTest
        @ValueSource(strings = ["XXX", ""])
        fun `should return no countries for unsupported currency`(currency: String) {
            val fry = preconditions { fry() }
            client.graphql { countries(currency = currency) }
                .from(fry)
                .executeAndVerifyResponse("countries" to buildJsonArray {})
        }
    }
}
