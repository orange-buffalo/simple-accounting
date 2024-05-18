package io.orangebuffalo.simpleaccounting.domain.users

import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldNotContain
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.*
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.ApiRequestsBodyConfiguration
import io.orangebuffalo.simpleaccounting.infra.utils.ApiRequestsValidationsTestBase
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.javacrumbs.jsonunit.assertj.JsonAssert
import org.hamcrest.CustomMatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * See [UserActivationTokensApiController] for the test subject.
 */
@SimpleAccountingIntegrationTest
@DisplayName("User Activation Token API ")
class UserActivationTokensApiControllerIT(
    @Autowired private val client: WebTestClient,
    @Autowired private val aggregateTemplate: JdbcAggregateTemplate,
    @Autowired private val timeService: TimeService,
    @Autowired private val preconditionsInfra: PreconditionsInfra
) {

    @BeforeEach
    fun setup() {
        mockCurrentTime(timeService)
    }

    @Nested
    @DisplayName("GET /api/user-activation-tokens/{userId}?by=userId")
    inner class GetUserActivationTokenByUserId {
        private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
            val expiredToken = userActivationToken(
                token = "expired-token",
                expiresAt = MOCK_TIME.minusSeconds(1)
            )

            val activeToken = userActivationToken(
                token = "active-token",
                expiresAt = MOCK_TIME.plusSeconds(1)
            )
        }

        private fun request(userId: Long = 42): WebTestClient.RequestHeadersSpec<*> {
            return client
                .get()
                .uri("/api/user-activation-tokens/{userId}?by=userId", userId)
        }

        @Test
        fun `should prohibit anonymous access`() {
            request()
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @WithMockFryUser
        fun `should require admin privileges`() {
            request()
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        @WithMockFarnsworthUser
        fun `should return 404 for non-existing token`() {
            request()
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @WithMockFarnsworthUser
        fun `should return 404 for expired token`() {
            val preconditions = setupPreconditions()

            request(userId = preconditions.expiredToken.userId)
                .exchange()
                .expectStatus().isNotFound

            withClue("Expired token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldContainOnly(preconditions.activeToken)
            }
        }

        @Test
        @WithMockFarnsworthUser
        fun `should return valid token`() {
            val preconditions = setupPreconditions()

            request(userId = preconditions.activeToken.userId)
                .verifyOkAndJsonBodyEqualTo {
                    put("token", "active-token")
                    put("expiresAt", "1999-03-28T23:01:03.042Z")
                }
        }
    }

    @Nested
    @DisplayName("GET /api/user-activation-tokens/{token}")
    inner class GetUserActivationToken {
        private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
            val expiredToken = userActivationToken(
                token = "expired-token",
                expiresAt = MOCK_TIME.minusSeconds(1)
            )

            val activeToken = userActivationToken(
                token = "active-token",
                expiresAt = MOCK_TIME.plusSeconds(1)
            )
        }

        private fun request(token: String): WebTestClient.RequestHeadersSpec<*> {
            return client
                .get()
                .uri("/api/user-activation-tokens/{token}", token)
        }

        @Test
        fun `should allow anonymous access`() {
            val preconditions = setupPreconditions()
            request(preconditions.activeToken.token)
                .exchange()
                .expectStatus().is2xxSuccessful
        }

        @Test
        @WithMockFryUser
        fun `should allow access with regular user privileges`() {
            val preconditions = setupPreconditions()
            request(preconditions.activeToken.token)
                .exchange()
                .expectStatus().is2xxSuccessful
        }

        @Test
        fun `should return 404 for non-existing token`() {
            request("non-existing-token")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        fun `should return 404 for expired token`() {
            val preconditions = setupPreconditions()
            request(preconditions.expiredToken.token)
                .exchange()
                .expectStatus().isNotFound

            withClue("Expired token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldContainOnly(preconditions.activeToken)
            }
        }

        @Test
        fun `should return valid token`() {
            val preconditions = setupPreconditions()
            request(preconditions.activeToken.token)
                .verifyOkAndJsonBodyEqualTo {
                    put("token", "active-token")
                    put("expiresAt", "1999-03-28T23:01:03.042Z")
                }
        }
    }

    @Nested
    @DisplayName("POST /api/user-activation-tokens")
    inner class CreateToken {
        private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
            val userWithoutToken = platformUser(
                activated = false
            )
            val userWithToken = platformUser(
                activated = false
            )
            val existingToken = userActivationToken(
                user = userWithToken,
                token = "existing-token",
                expiresAt = MOCK_TIME.plusSeconds(1)
            )
            val activatedUser = platformUser(
                activated = true
            )
        }

        private fun request(userId: Long = 42): WebTestClient.RequestHeadersSpec<*> {
            return client
                .post()
                .uri("/api/user-activation-tokens")
                .sendJson {
                    put("userId", userId)
                }
        }

        @Test
        fun `should prohibit anonymous access`() {
            request()
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @WithMockFryUser
        fun `should require admin privileges`() {
            request()
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        @WithMockFarnsworthUser
        fun `should return 404 for non-existing user`() {
            request(100500)
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @WithMockFarnsworthUser
        fun `should return 400 when trying to create a token for activated user`() {
            val preconditions = setupPreconditions()
            request(preconditions.activatedUser.id!!)
                .exchange()
                .expectStatus().isBadRequest
                .expectThatJsonBodyEqualTo {
                    put("error", "UserAlreadyActivated")
                    put("message", "User ${preconditions.activatedUser.id} is already activated")
                }
        }

        @Test
        @WithMockFarnsworthUser
        fun `should create token for user without token`() {
            val preconditions = setupPreconditions()
            request(userId = preconditions.userWithoutToken.id!!)
                .exchange()
                .expectStatus().isCreated
                .expectThatJsonBody {
                    withDbTokenValueForUserMatcher(preconditions.userWithoutToken)
                        .isEqualToJson {
                            put("token", "#{json-unit.matches:dbTokenValue}")
                            put("expiresAt", "1999-03-29T04:01:02.042Z")
                        }
                }
        }

        @Test
        @WithMockFarnsworthUser
        fun `should create new token for user with existing token`() {
            val preconditions = setupPreconditions()
            request(userId = preconditions.userWithToken.id!!)
                .exchange()
                .expectStatus().isCreated
                .expectThatJsonBody {
                    withDbTokenValueForUserMatcher(preconditions.userWithToken)
                        .isEqualToJson {
                            put("token", "#{json-unit.matches:dbTokenValue}")
                            put("expiresAt", "1999-03-29T04:01:02.042Z")
                        }
                }

            withClue("Existing token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldNotContain(preconditions.existingToken)
            }
        }
    }

    @Nested
    @DisplayName("POST /api/user-activation-tokens/{token}/activate")
    inner class ActivateUser {
        private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
            val expiredToken = userActivationToken(
                token = "expired-token",
                expiresAt = MOCK_TIME.minusSeconds(1)
            )
            val user = platformUser(
                activated = false
            )
            val activeToken = userActivationToken(
                user = user,
                token = "active-token",
                expiresAt = MOCK_TIME.plusSeconds(1)
            )
        }

        private fun request(
            token: String,
            password: String = "qwerty",
            body: String = buildJsonObject {
                put("password", password)
            }.toString()
        ): WebTestClient.RequestHeadersSpec<*> {
            return client
                .post()
                .uri("/api/user-activation-tokens/{token}/activate", token)
                .sendJson(body)
        }

        @Test
        fun `should allow anonymous access`() {
            val preconditions = setupPreconditions()
            request(preconditions.activeToken.token)
                .exchange()
                .expectStatus().is2xxSuccessful
        }

        @Test
        @WithMockFryUser
        fun `should allow access with regular user privileges`() {
            val preconditions = setupPreconditions()
            request(preconditions.activeToken.token)
                .exchange()
                .expectStatus().is2xxSuccessful
        }

        @Test
        fun `should return 404 for non-existing token`() {
            request("non-existing-token")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        fun `should return 400 for expired token`() {
            val preconditions = setupPreconditions()
            request(preconditions.expiredToken.token)
                .exchange()
                .expectStatus().isBadRequest
                .expectThatJsonBodyEqualTo {
                    put("error", "TokenExpired")
                    put("message", "Token expired")
                }

            withClue("Expired token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldContainOnly(preconditions.activeToken)
            }
        }

        @Test
        fun `should activate account when valid token is used`() {
            val preconditions = setupPreconditions()

            request(preconditions.activeToken.token)
                .verifyOkNoContent()

            withClue("User should be activated") {
                val user = aggregateTemplate.findById(preconditions.user.id!!, PlatformUser::class.java)
                user.activated.shouldBeTrue()
            }

            withClue("Token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldNotContain(preconditions.activeToken)
            }
        }

        @Nested
        inner class RequestsValidation : ApiRequestsValidationsTestBase() {
            override val requestExecutionSpec = { requestBody: String ->
                val preconditions = setupPreconditions()
                request(preconditions.activeToken.token, body = requestBody)
            }

            override val requestBodySpec: ApiRequestsBodyConfiguration = {
                string("password", maxLength = 100, mandatory = true)
            }

            override val successResponseStatus = HttpStatus.NO_CONTENT
        }
    }

    private fun JsonAssert.ConfigurableJsonAssert.withDbTokenValueForUserMatcher(user: PlatformUser) =
        withMatcher("dbTokenValue", object : CustomMatcher<String>("dbTokenValue") {
            override fun matches(item: Any): Boolean {
                val token = aggregateTemplate.findAll(UserActivationToken::class.java)
                    .single { it.userId == user.id }
                return item == token.token
            }
        })
}
