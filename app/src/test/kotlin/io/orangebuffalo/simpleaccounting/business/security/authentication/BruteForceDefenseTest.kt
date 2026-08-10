package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.kotest.assertions.fail
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.users.LoginStatistics
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersRepository
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.support.TransactionTemplate
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.concurrent.Executors

private val CURRENT_TIME = Instant.ofEpochMilli(424242)

class BruteForceDefenseTest(
    @Autowired private val client: ApiTestClient,
    @Autowired private val transactionTemplate: TransactionTemplate,
    @Autowired private val platformUsersRepository: PlatformUsersRepository,
    @Value($$"${local.server.port}") private val serverPort: Int,
) : SaIntegrationTestBase() {

    @BeforeEach
    fun setupCurrentTime() {
        doReturn(CURRENT_TIME).whenever(timeService).currentTime()
    }

    @Test
    fun `should successfully login if account is unlocked`() {
        setupPreconditions()
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        client
            .graphqlMutation { loginMutation() }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.CreateAccessTokenByCredentials to buildJsonObject {
                    put("accessToken", "\${json-unit.any-string}")
                }
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(0)
            temporaryLockExpirationTime.shouldBeNull()
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(1)
            temporaryLockExpirationTime.shouldBeNull()
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
                DgsConstants.MUTATION.CreateAccessTokenByCredentials to buildJsonObject {
                    put("accessToken", "\${json-unit.any-string}")
                }
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(0)
            temporaryLockExpirationTime.shouldBeNull()
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials,
                additionalExtensions = mapOf("lockExpiresInSec" to 0)
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(5)
            temporaryLockExpirationTime.shouldBe(CURRENT_TIME)
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials,
                additionalExtensions = mapOf("lockExpiresInSec" to 4)
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(5)
            temporaryLockExpirationTime.shouldBe(CURRENT_TIME.plusMillis(4500))
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(5)
            temporaryLockExpirationTime.shouldBeNull()
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials,
                additionalExtensions = mapOf("lockExpiresInSec" to 60)
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(6)
            temporaryLockExpirationTime.shouldBe(CURRENT_TIME.plusSeconds(60))
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials,
                additionalExtensions = mapOf("lockExpiresInSec" to 135)
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(8)
            temporaryLockExpirationTime.shouldBe(CURRENT_TIME.plusMillis(135_000))
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials,
                additionalExtensions = mapOf("lockExpiresInSec" to 86400)
            )

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(101)
            temporaryLockExpirationTime.shouldBe(CURRENT_TIME.plusMillis(86_400_000))
        }
    }

    @Test
    fun `should handle parallel login requests and throttle them`() {
        setupPreconditions()
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        val httpClient = HttpClient.newHttpClient()
        val responses = Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            List(10) {
                executor.submit<HttpResponse<String>> {
                    httpClient.executeGraphqlLoginForFry()
                }
            }.map { it.get() }
        }

        var badCredentialsCount = 0
        var loginNotAvailableCount = 0
        var accountLockedCount = 0
        responses.forEach { response ->
            response.statusCode().shouldBe(200)
            val json = response.body()
            when {
                json.contains("BAD_CREDENTIALS") -> badCredentialsCount++
                json.contains("LOGIN_NOT_AVAILABLE") -> loginNotAvailableCount++
                json.contains("ACCOUNT_LOCKED") -> accountLockedCount++
                else -> fail("[$json] is not an expected error")
            }
        }

        // we can't know how exactly each request is processed, but overall all issued requests must be responded
        (badCredentialsCount + loginNotAvailableCount + accountLockedCount).shouldBe(10)
        // at least one must go through and fail with Bad Credentials
        badCredentialsCount.shouldBeGreaterThan(0)

        assertFryLoginStatistics {
            // depending on how many requests we process to login, different number of failed attempts is possible
            // but the number of Bad Credentials responses should be equal to failed attempts number
            if (accountLockedCount > 0) {
                failedAttemptsCount.shouldBe(6)
            } else {
                failedAttemptsCount.shouldBe(badCredentialsCount)
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

    private fun setupFryLoginStatistics(spec: MutableLoginStatistics.() -> Unit) {
        transactionTemplate.execute {
            val fry = platformUsersRepository.findByUserName("Fry")
                ?: throw IllegalStateException("Fry is not found?!")
            val loginStatistics = MutableLoginStatistics(
                failedAttemptsCount = fry.loginStatistics.failedAttemptsCount,
                temporaryLockExpirationTime = fry.loginStatistics.temporaryLockExpirationTime,
            ).apply(spec)
            platformUsersRepository.save(
                fry.copy(
                    loginStatistics = LoginStatistics(
                        failedAttemptsCount = loginStatistics.failedAttemptsCount,
                        temporaryLockExpirationTime = loginStatistics.temporaryLockExpirationTime,
                    )
                )
            )
        }
    }

    private data class MutableLoginStatistics(
        var failedAttemptsCount: Int,
        var temporaryLockExpirationTime: Instant?,
    )

    private fun MutationProjection.loginMutation(): MutationProjection =
        createAccessTokenByCredentials(password = "qwerty", userName = "Fry") { accessToken }

    /**
     * We use the JDK client here to issue truly parallel requests bypassing ApiTestClient's
     * JWT-based authentication and single-threaded request processing.
     */
    private fun HttpClient.executeGraphqlLoginForFry(): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI("http://localhost:$serverPort/api/graphql"))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"query\":\"mutation { createAccessTokenByCredentials(userName: \\\"Fry\\\", " +
                        "password: \\\"qwerty\\\") { accessToken } }\"}"
                )
            )
            .build()
        return send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun setupPreconditions() = preconditions {
        fry()
    }
}
