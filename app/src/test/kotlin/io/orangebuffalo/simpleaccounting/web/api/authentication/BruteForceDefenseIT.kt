@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.orangebuffalo.simpleaccounting.web.api.authentication

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import assertk.assertions.isZero
import assertk.fail
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.api.expectThatJsonBody
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.LoginStatistics
import io.orangebuffalo.simpleaccounting.services.persistence.repos.PlatformUserRepository
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import kotlinx.coroutines.*
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

private const val LOGIN_PATH = "/api/auth/login"
private val CURRENT_TIME = Instant.ofEpochMilli(424242)

@SimpleAccountingIntegrationTest
class BruteForceDefenseIT(
    @Autowired val client: WebTestClient,
    @Autowired val transactionTemplate: TransactionTemplate,
    @Autowired val platformUserRepository: PlatformUserRepository,
    @Autowired val passwordEncoder: PasswordEncoder,
) {
    @MockBean
    lateinit var timeService: TimeService

    @BeforeEach
    fun setupCurrentTime() {
        whenever(timeService.currentTime()) doReturn CURRENT_TIME
    }

    @Test
    fun `should successfully login if account is unlocked`(testData: BruteForceDefenseTestData) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        client.executeLoginForFry()
            .expectStatus().isOk

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isZero()
            assertThat(temporaryLockExpirationTime).isNull()
        }
    }

    @Test
    fun `should not lock account after the first failure`(testData: BruteForceDefenseTestData) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client.executeLoginForFry()
            .expectStatus().isUnauthorized
            .assertJsonResponse(
                """{
                    "error": "BadCredentials"
                }"""
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(1)
            assertThat(temporaryLockExpirationTime).isNull()
        }
    }

    @Test
    fun `should successfully login if account is temporary locked but lock has expired`(
        testData: BruteForceDefenseTestData
    ) {
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = CURRENT_TIME.minusMillis(1)
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        client.executeLoginForFry()
            .expectStatus().isOk

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isZero()
            assertThat(temporaryLockExpirationTime).isNull()
        }
    }

    @Test
    fun `should forbid login if account is temporary locked (boundary case of the last millis)`(
        testData: BruteForceDefenseTestData
    ) {
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = CURRENT_TIME
        }

        client.executeLoginForFry()
            .expectStatus().isUnauthorized
            .assertJsonResponse(
                """{
                    "error": "AccountLocked",
                    "lockExpiresInSec": 0
                }"""
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(5)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME)
        }
    }

    @Test
    fun `should forbid login if account is temporary locked`(testData: BruteForceDefenseTestData) {
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = CURRENT_TIME.plusMillis(4500)
        }

        client.executeLoginForFry()
            .expectStatus().isUnauthorized
            .assertJsonResponse(
                """{
                    "error": "AccountLocked",
                    "lockExpiresInSec": 4
                }"""
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(5)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME.plusMillis(4500))
        }
    }

    @Test
    fun `should increase failed attempts without locking if below 5 attempts`(testData: BruteForceDefenseTestData) {
        setupFryLoginStatistics {
            failedAttemptsCount = 4
            temporaryLockExpirationTime = null
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client.executeLoginForFry()
            .expectStatus().isUnauthorized
            .assertJsonResponse(
                """{
                    "error": "BadCredentials"
                }"""
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(5)
            assertThat(temporaryLockExpirationTime).isNull()
        }
    }

    @Test
    fun `should lock account after 5 failed attempts`(testData: BruteForceDefenseTestData) {
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = null
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client.executeLoginForFry()
            .expectStatus().isUnauthorized
            .assertJsonResponse(
                """{
                    "error": "AccountLocked",
                    "lockExpiresInSec": 60
                }"""
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(6)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME.plusSeconds(60))
        }
    }

    @Test
    fun `should progressively increase locking time`(testData: BruteForceDefenseTestData) {
        setupFryLoginStatistics {
            failedAttemptsCount = 7
            temporaryLockExpirationTime = CURRENT_TIME.minusMillis(1)
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client.executeLoginForFry()
            .expectStatus().isUnauthorized
            .assertJsonResponse(
                """{
                    "error": "AccountLocked",
                    "lockExpiresInSec": 135
                }"""
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(8)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME.plusMillis(135_000))
        }
    }

    @Test
    fun `should cap locking time at 1 day`(testData: BruteForceDefenseTestData) {
        setupFryLoginStatistics {
            failedAttemptsCount = 100
            temporaryLockExpirationTime = CURRENT_TIME.minusMillis(1)
        }

        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client.executeLoginForFry()
            .expectStatus().isUnauthorized
            .assertJsonResponse(
                """{
                    "error": "AccountLocked",
                    "lockExpiresInSec": 86400
                }"""
            )

        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(101)
            assertThat(temporaryLockExpirationTime).isEqualTo(CURRENT_TIME.plusMillis(86_400_000))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `should handle parallel login requests and throttle them`(testData: BruteForceDefenseTestData) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        val requests = generateSequence(1) { if (it < 10) it + 1 else null }
            .map {
                GlobalScope.async(newFixedThreadPoolContext(10, "parallelLogins")) {
                    client.executeLoginForFry()
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
                .expectStatus().isUnauthorized
                .expectBody<String>().consumeWith { body ->
                    val json = body.responseBody ?: ""
                    when {
                        json.contains("BadCredentials") -> badCredentialsCount++
                        json.contains("LoginNotAvailable") -> loginNotAvailableCount++
                        json.contains("AccountLocked") -> accountLockedCount++
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
            val loginStatistics = platformUserRepository.findByUserName("Fry")?.loginStatistics
                ?: throw IllegalStateException("Fry is not found?!")
            loginStatistics.spec()
        }
    }

    private fun setupFryLoginStatistics(spec: LoginStatistics.() -> Unit) {
        transactionTemplate.execute {
            val fry = platformUserRepository.findByUserName("Fry")
                ?: throw IllegalStateException("Fry is not found?!")
            fry.loginStatistics.spec()
            platformUserRepository.save(fry)
        }
    }

    private fun WebTestClient.ResponseSpec.assertJsonResponse(json: String) {
        expectThatJsonBody {
            isEqualTo(json(json))
        }
    }

    private fun WebTestClient.executeLoginForFry(): WebTestClient.ResponseSpec = post()
        .uri(LOGIN_PATH)
        .contentType(APPLICATION_JSON)
        .bodyValue(
            LoginRequest(
                userName = "Fry",
                password = "qwerty"
            )
        )
        .exchange()

    class BruteForceDefenseTestData : TestData {
        val fry = Prototypes.fry()

        override fun generateData() = listOf(fry)
    }
}
