package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import io.orangebuffalo.simpleaccounting.business.countries.CountryFinancialRegistry
import java.util.Locale

class CountriesQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {
    @Test
    fun `should return all ISO countries for workspace setup without an existing workspace`() {
        val fry = preconditions { fry() }
        client.graphql { countries(currency = null) }
            .from(fry)
            .executeAndVerifyResponse("countries" to buildJsonArray {
                Locale.getISOCountries().forEach { add(it) }
            })
    }

    @Test
    fun `should require authentication`() {
        client.graphql { countries(currency = null) }
            .fromAnonymous()
            .executeAndVerifyNotAuthorized(path = "countries")
    }

    @ParameterizedTest
    @ValueSource(strings = ["AUD", "USD", "EUR", "INR", "ZAR", "XXX", ""])
    fun `should restrict countries by supported currency`(currency: String) {
        val fry = preconditions { fry() }
        client.graphql { countries(currency = currency) }
            .from(fry)
            .executeAndVerifyResponse("countries" to buildJsonArray {
                CountryFinancialRegistry.byResidency
                    .filterValues { currency in it.supportedCurrencies }
                    .keys.forEach { add(it) }
            })
    }

    @Test
    fun `should require authentication for currency filtered countries`() {
        client.graphql { countries(currency = "AUD") }
            .fromAnonymous()
            .executeAndVerifyNotAuthorized(path = "countries")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "AUD"])
    fun `should reject admin users`(currency: String) {
        val farnsworth = preconditions { farnsworth() }
        client.graphql { countries(currency = currency.ifEmpty { null }) }
            .from(farnsworth)
            .executeAndVerifyNotAuthorized(path = "countries")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "AUD"])
    fun `should reject workspace access tokens`(currency: String) {
        val token = preconditions {
            workspaceAccessToken(validTill = MOCK_TIME.plusSeconds(10000))
        }
        client.graphql { countries(currency = currency.ifEmpty { null }) }
            .usingSharedWorkspaceToken(token.token)
            .executeAndVerifyNotAuthorized(path = "countries")
    }
}
