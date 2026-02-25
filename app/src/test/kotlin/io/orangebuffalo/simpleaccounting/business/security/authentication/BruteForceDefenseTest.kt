@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.orangebuffalo.simpleaccounting.business.security.authentication

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import assertk.assertions.isZero
import assertk.fail
import io.orangebuffalo.simpleaccounting.business.users.LoginStatistics
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersRepository
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import kotlinx.coroutines.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

private val CURRENT_TIME = Instant.ofEpochMilli(424242)

class BruteForceDefenseTest(
    @Autowired private val client: ApiTestClient,
    @Autowired private val rawClient: WebTestClient,
    @Autowired private val transactionTemplate: TransactionTemplate,
    @Autowired private val platformUsersRepository: PlatformUsersRepository,
) : SaIntegrationTestBase() {

    @BeforeEach
    fun setupCurrentTime() {
        whenever(timeService.currentTime()) doReturn CURRENT_TIME
    }

    @Test
    fun `should successfully login if account is unlocked`() {
        setupPreconditions()
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                "createAccessTokenByCredentials" to buildJsonObject {
                    put("accessToken", "\${json-unit.any-string}")
                }
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isZero()
            assertThat(temporaryLockExpirationTime).isNull()
        }
    }

    @Test
    fun `should not lock account after the first failure`() {
        setupPreconditions()
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Invalid Credentials",
                errorCode = "BAD_CREDENTIALS",
                path = "createAccessTokenByCredentials"
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(1)
            assertThat(temporaryLockExpirationTime).isNull()
        }
    }

    @Test
    fun `should successfully login if account is temporary locked but lock has expired`() {
        setupPreconditions()
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = CURRENT_TIME.minusMillis(1)
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                "createAccessTokenByCredentials" to buildJsonObject {
                    put("accessToken", "\${json-unit.any-string}")
                }
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isZero()
            assertThat(temporaryLockExpirationTime).isNull()
        }
    }

    @Test
    fun `should forbid login if account is temporary locked (boundary case of the last millis)`() {
        setupPreconditions()
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = CURRENT_TIME
        }

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Account is temporary locked",
                errorCode = "ACCOUNT_LOCKED",
                path = "createAccessTokenByCredentials",
                additionalExtensions = mapOf("lockExpiresInSec" to 0)
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(5)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME)
        }
    }

    @Test
    fun `should forbid login if account is temporary locked`() {
        setupPreconditions()
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = CURRENT_TIME.plusMillis(4500)
        }

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Account is temporary locked",
                errorCode = "ACCOUNT_LOCKED",
                path = "createAccessTokenByCredentials",
                additionalExtensions = mapOf("lockExpiresInSec" to 4)
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(5)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME.plusMillis(4500))
        }
    }

    @Test
    fun `should increase failed attempts without locking if below 5 attempts`() {
        setupPreconditions()
        setupFryLoginStatistics {
            failedAttemptsCount = 4
            temporaryLockExpirationTime = null
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Invalid Credentials",
                errorCode = "BAD_CREDENTIALS",
                path = "createAccessTokenByCredentials"
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(5)
            assertThat(temporaryLockExpirationTime).isNull()
        }
    }

    @Test
    fun `should lock account after 5 failed attempts`() {
        setupPreconditions()
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = null
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Account is temporary locked",
                errorCode = "ACCOUNT_LOCKED",
                path = "createAccessTokenByCredentials",
                additionalExtensions = mapOf("lockExpiresInSec" to 60)
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(6)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME.plusSeconds(60))
        }
    }

    @Test
    fun `should progressively increase locking time`() {
        setupPreconditions()
        setupFryLoginStatistics {
            failedAttemptsCount = 7
            temporaryLockExpirationTime = CURRENT_TIME.minusMillis(1)
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Account is temporary locked",
                errorCode = "ACCOUNT_LOCKED",
                path = "createAccessTokenByCredentials",
                additionalExtensions = mapOf("lockExpiresInSec" to 135)
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(8)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME.plusMillis(135_000))
        }
    }

    @Test
    fun `should cap locking time at 1 day`() {
        setupPreconditions()
        setupFryLoginStatistics {
            failedAttemptsCount = 100
            temporaryLockExpirationTime = CURRENT_TIME.minusMillis(1)
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Account is temporary locked",
                errorCode = "ACCOUNT_LOCKED",
                path = "createAccessTokenByCredentials",
                additionalExtensions = mapOf("lockExpiresInSec" to 86400)
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(101)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME.plusMillis(86_400_000))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `should handle parallel login requests and throttle them`() {
        setupPreconditions()
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        val requests = generateSequence(1) { if (it < 10) it + 1 else null }
            .map {
                GlobalScope.async(newFixedThreadPoolContext(10, "parallelLogins")) {
                    rawClient.executeGraphqlLoginForFry()
                }
            }
            .toList()

        val responses = runBlocking {
            requests.awaitAll()
        }

        var badCredentialsCount = 0
        var loginNotAvailableCount = 0
        var accountLockedCount = 0
        responses.forEach { response ->
            response
                .expectStatus().isOk
                .expectBody<String>().consumeWith { body ->
                    val json = body.responseBody ?: ""
                    when {
                        json.contains("BAD_CREDENTIALS") -> badCredentialsCount++
                        json.contains("LOGIN_NOT_AVAILABLE") -> loginNotAvailableCount++
                        json.contains("ACCOUNT_LOCKED") -> accountLockedCount++
                        else -> fail("[$json] is not an expected error")
                    }
                }
        }

        // we can't know how exactly each request is processed, but overall all issued requests must be responded
        assertThat(badCredentialsCount + loginNotAvailableCount + accountLockedCount).isEqualTo(10)
        // at least one must go through and fail with Bad Credentials
        assertThat(badCredentialsCount).isGreaterThan(0)

        assertFryLoginStatistics {
            // depending on how many requests we process to login, different number of failed attempts is possible
            // but the number of Bad Credentials responses should be equal to failed attempts number
            if (accountLockedCount > 0) {
                assertThat(failedAttemptsCount).isEqualTo(6)
            } else {
                assertThat(failedAttemptsCount).isEqualTo(badCredentialsCount)
            }
        }
    }

    private fun assertFryLoginStatistics(spec: LoginStatistics.() -> Unit) {
        transactionTemplate.execute {
            val loginStatistics = platformUsersRepository.findByUserName("Fry")?.loginStatistics
                ?: throw IllegalStateException("Fry is not found?!")
            loginStatistics.spec()
        }
    }

    private fun setupFryLoginStatistics(spec: LoginStatistics.() -> Unit) {
        transactionTemplate.execute {
            val fry = platformUsersRepository.findByUserName("Fry")
                ?: throw IllegalStateException("Fry is not found?!")
            fry.loginStatistics.spec()
            platformUsersRepository.save(fry)
        }
    }

    private fun MutationProjection.loginMutation(): MutationProjection =
        createAccessTokenByCredentials(password = "qwerty", userName = "Fry") { accessToken }

    /**
     * We use raw WebTestClient here to issue truly parallel requests bypassing ApiTestClient's
     * JWT-based authentication and single-threaded request processing.
     */
    private fun WebTestClient.executeGraphqlLoginForFry(): WebTestClient.ResponseSpec = post()
        .uri("/api/graphql")
        .header("Content-Type", "application/json")
        .bodyValue("""{"query": "mutation { createAccessTokenByCredentials(userName: \"Fry\", password: \"qwerty\") { accessToken } }"}""")
        .exchange()

    private fun setupPreconditions() = preconditions {
        fry()
    }
}
