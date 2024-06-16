package io.orangebuffalo.simpleaccounting.domain.users

import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.*
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant

@SimpleAccountingIntegrationTest
@DisplayName("Users API")
internal class UsersApiControllerIT(
    @Autowired private val client: WebTestClient,
    @Autowired private val aggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val timeService: TimeService,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @Nested
    @DisplayName("GET /api/users")
    inner class GetUsers {

        private fun sendRequest() = client.get().uri("/api/users")

        @Test
        fun `should prohibit anonymous access`() {
            sendRequest()
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @WithMockFryUser
        fun `should prohibit access by regular users`() {
            sendRequest()
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        @WithMockFarnsworthUser
        fun `should return a valid users page`() {
            val testData = setupPreconditions()
            sendRequest()
                .verifyOkAndJsonBodyEqualTo {
                    put("pageNumber", 1)
                    put("pageSize", 10)
                    put("totalElements", 3)
                    putJsonArray("data") {
                        addJsonObject {
                            put("userName", "Farnsworth")
                            put("id", testData.farnsworth.id)
                            put("version", 0)
                            put("admin", true)
                            put("activated", true)
                        }
                        addJsonObject {
                            put("userName", "Fry")
                            put("id", testData.fry.id)
                            put("version", 0)
                            put("admin", false)
                            put("activated", true)
                        }
                        addJsonObject {
                            put("userName", "Zoidberg")
                            put("id", testData.zoidberg.id)
                            put("version", 0)
                            put("admin", false)
                            put("activated", false)
                        }
                    }
                }
        }

        private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
            val farnsworth = farnsworth()
            val fry = fry()
            val zoidberg = platformUser(
                userName = "Zoidberg",
                isAdmin = false,
                activated = false
            )
        }
    }

    @Nested
    @DisplayName("POST /api/users")
    inner class CreateUser {

        @Test
        fun `should prohibit anonymous access`() {
            sendRequest()
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @WithMockFryUser
        fun `should prohibit regular user access`() {
            sendRequest()
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        @WithMockFarnsworthUser
        fun `should create a new user`() {
            setupPreconditions()
            mockCurrentTime(timeService)

            sendRequest()
                .verifyOkAndJsonBodyEqualTo {
                    put("userName", "Leela")
                    put("id", "#{json-unit.any-number}")
                    put("version", 0)
                    put("admin", false)
                    put("activated", false)
                }

            val createdUserId = aggregateTemplate.findAll<PlatformUser>()
                .filter { it.userName == "Leela" }
                .shouldBeSingle()
                .id

            aggregateTemplate.findSingle<UserActivationToken>()
                .should {
                    it.shouldBeEntityWithFields(
                        UserActivationToken(
                            userId = createdUserId!!,
                            token = it.token,
                            expiresAt = Instant.now()
                        ),
                        ignoredProperties = arrayOf(UserActivationToken::expiresAt)
                    )
                    it.expiresAt.shouldBeEqualComparingTo(MOCK_TIME.plusSeconds(5 * 3600))
                }
        }

        private fun sendRequest() = client
            .post()
            .uri("/api/users")
            .sendJson {
                put("userName", "Leela")
                put("admin", false)
            }

        private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
            init {
                farnsworth()
            }
        }
    }
}
