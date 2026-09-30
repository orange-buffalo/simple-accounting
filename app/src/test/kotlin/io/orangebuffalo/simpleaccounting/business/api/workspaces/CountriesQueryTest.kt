package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Locale

class CountriesQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {
    @Test
    fun `should return all ISO countries for workspace setup without an existing workspace`() {
        val fry = preconditions { fry() }
        client.graphql { countries }
            .from(fry)
            .executeAndVerifyResponse("countries" to buildJsonArray {
                Locale.getISOCountries().forEach { add(it) }
            })
    }

    @Test
    fun `should require authentication`() {
        client.graphql { countries }
            .fromAnonymous()
            .executeAndVerifyNotAuthorized(path = "countries")
    }
}
