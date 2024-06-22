package io.orangebuffalo.simpleaccounting.domain.users

import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.*
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
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
import org.springframework.http.HttpStatus
import java.time.Instant

@SimpleAccountingIntegrationTest
@DisplayName("Users API")
internal class UsersApiControllerIT(
    @Autowired private val client: ApiTestClient,
    @Autowired private val aggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val timeService: TimeService,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    /**
     * [UsersApiController.getUsers]
     */
    @Nested
    @DisplayName("GET /api/users")
    inner class GetUsers {
        private val preconditions by lazy {
            object : Preconditions(preconditionsInfra) {
                val farnsworth = farnsworth()
                val fry = fry()
                val zoidberg = platformUser(
                    userName = "Zoidberg",
                    isAdmin = false,
                    activated = false
                )
            }
        }

        private fun request() = client
            .get()
            .uri("/api/users")

        @Test
        fun `should prohibit anonymous access`() {
            request()
                .fromAnonymous()
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `should prohibit access by regular users`() {
            request()
                .from(preconditions.fry)
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `should return a valid users page`() {
            request()
                .from(preconditions.farnsworth)
                .verifyOkAndJsonBodyEqualTo {
                    put("pageNumber", 1)
                    put("pageSize", 10)
                    put("totalElements", 3)
                    putJsonArray("data") {
                        addJsonObject {
                            put("userName", "Farnsworth")
                            put("id", preconditions.farnsworth.id)
                            put("version", 0)
                            put("admin", true)
                            put("activated", true)
                        }
                        addJsonObject {
                            put("userName", "Fry")
                            put("id", preconditions.fry.id)
                            put("version", 0)
                            put("admin", false)
                            put("activated", true)
                        }
                        addJsonObject {
                            put("userName", "Zoidberg")
                            put("id", preconditions.zoidberg.id)
                            put("version", 0)
                            put("admin", false)
                            put("activated", false)
                        }
                    }
                }
        }
    }

    /**
     * [UsersApiController.createUser]
     */
    @Nested
    @DisplayName("POST /api/users")
    inner class CreateUser {

        private val preconditions by lazy {
            object : Preconditions(preconditionsInfra) {
                val farnsworth = farnsworth()
                val fry = fry()
            }
        }

        private fun request() = client
            .post()
            .uri("/api/users")
            .sendJson {
                put("userName", "Leela")
                put("admin", false)
            }

        @Test
        fun `should prohibit anonymous access`() {
            request()
                .fromAnonymous()
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `should prohibit regular user access`() {
            request()
                .from(preconditions.fry)
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `should create a new user`() {
            mockCurrentTime(timeService)

            request()
                .from(preconditions.farnsworth)
                .verifyCreatedAndJsonBodyEqualTo {
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

        @Nested
        inner class RequestsValidation : ApiRequestsValidationsTestBase() {
            override val requestExecutionSpec = { requestBody: String ->
                client
                    .post()
                    .uri("/api/users")
                    .sendJson(requestBody)
                    .from(preconditions.farnsworth)
            }

            override val requestBodySpec: ApiRequestsBodyConfiguration = {
                string("userName", maxLength = 255, mandatory = true)
                boolean("admin", mandatory = true)
            }

            override val successResponseStatus = HttpStatus.CREATED
        }
    }
}
