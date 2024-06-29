package io.orangebuffalo.simpleaccounting.domain.users

import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.*
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsExtension
import io.orangebuffalo.simpleaccounting.infra.utils.*
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.HttpStatus
import java.time.Instant

@SimpleAccountingIntegrationTest
@DisplayName("Users API")
internal class UsersApiControllerIT(
    @Autowired private val client: ApiTestClient,
    @Autowired private val aggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val timeService: TimeService
) {

    /**
     * [UsersApiController.getUsers]
     */
    @Nested
    @DisplayName("GET /api/users")
    inner class GetUsers {
        @RegisterExtension
        private val preconditionsExt = PreconditionsExtension {
            object {
                val farnsworth = farnsworth()
                val fry = fry()
                val zoidberg = platformUser(
                    userName = "Zoidberg",
                    isAdmin = false,
                    activated = false
                )
            }
        }
        private val preconditions by preconditionsExt

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

        @RegisterExtension
        private val preconditionsExt = PreconditionsExtension {
            object {
                val farnsworth = farnsworth()
                val fry = fry()
            }
        }
        private val preconditions by preconditionsExt

        private fun request(userName: String = "Leela") = client
            .post()
            .uri("/api/users")
            .sendJson {
                put("userName", userName)
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

        @Test
        fun `should not allow to create duplicated user`() {
            request(userName = preconditions.farnsworth.userName)
                .from(preconditions.farnsworth)
                .verifyBadRequestAndJsonBodyEqualTo {
                    put("error", "UserAlreadyExists")
                    put("message", "User with name '${preconditions.farnsworth.userName}' already exists")
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

    /**
     * [UsersApiController.updateUser]
     */
    @Nested
    @DisplayName("PUT /api/users/{userId}")
    inner class UpdateUser {

        @RegisterExtension
        private val preconditionsExt = PreconditionsExtension {
            object {
                val farnsworth = farnsworth()
                val fry = fry()
            }
        }
        private val preconditions by preconditionsExt

        private fun request(userId: Long?, userName: String = "Leela") = client
            .put()
            .uri("/api/users/${userId}")
            .sendJson {
                put("userName", userName)
            }

        @Test
        fun `should prohibit anonymous access`() {
            request(userId = preconditions.fry.id)
                .fromAnonymous()
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `should prohibit regular user access`() {
            request(userId = preconditions.fry.id)
                .from(preconditions.fry)
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        fun `should update user`() {
            request(userId = preconditions.fry.id, userName = "Leela")
                .from(preconditions.farnsworth)
                .verifyOkAndJsonBodyEqualTo {
                    put("userName", "Leela")
                    put("id", preconditions.fry.id)
                    put("version", 1)
                    put("admin", false)
                    put("activated", true)
                }

            aggregateTemplate.findAll<PlatformUser>()
                .filter { it.id == preconditions.fry.id }
                .shouldBeSingle()
                .should {
                    it.userName.shouldBe("Leela")
                }
        }

        @Test
        fun `should not allow to update to existing user name`() {
            request(userId = preconditions.fry.id, userName = preconditions.farnsworth.userName)
                .from(preconditions.farnsworth)
                .verifyBadRequestAndJsonBodyEqualTo {
                    put("error", "UserAlreadyExists")
                    put("message", "User with name '${preconditions.farnsworth.userName}' already exists")
                }
        }

        @Nested
        inner class RequestsValidation : ApiRequestsValidationsTestBase() {
            override val requestExecutionSpec = { requestBody: String ->
                client
                    .put()
                    .uri("/api/users/${preconditions.fry.id}")
                    .sendJson(requestBody)
                    .from(preconditions.farnsworth)
            }

            override val requestBodySpec: ApiRequestsBodyConfiguration = {
                string("userName", maxLength = 255, mandatory = true)
            }
        }
    }
}
