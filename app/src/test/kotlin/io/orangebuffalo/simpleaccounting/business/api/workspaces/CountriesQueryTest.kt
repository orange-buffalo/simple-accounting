package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
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
        fun `should return countries for workspace setup without an existing workspace`() {
            val fry = preconditions { fry() }
            client.graphql { countries(currency = null) }
                .from(fry)
                .execute().expectStatus().isOk.expectBody()
                .jsonPath("$.errors").doesNotExist()
                .jsonPath("$.data.countries[?(@ == 'AU')]").isNotEmpty()
                .jsonPath("$.data.countries[?(@ == 'US')]").isNotEmpty()
        }

        @ParameterizedTest
        @CsvSource(
            delimiter = '|',
            value = [
                "AUD | AU | CC | US",
                "USD | US | HT | AU",
                "EUR | DE | PL | US",
                "INR | IN | BT | US",
                "ZAR | ZA | NA | US",
            ],
        )
        fun `should restrict countries by supported currency`(
            currency: String, firstCountry: String, secondCountry: String, excludedCountry: String,
        ) {
            val fry = preconditions { fry() }
            client.graphql { countries(currency = currency) }
                .from(fry)
                .execute().expectStatus().isOk.expectBody()
                .jsonPath("$.errors").doesNotExist()
                .jsonPath("$.data.countries[?(@ == '$firstCountry')]").isNotEmpty()
                .jsonPath("$.data.countries[?(@ == '$secondCountry')]").isNotEmpty()
                .jsonPath("$.data.countries[?(@ == '$excludedCountry')]").isEmpty()
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
