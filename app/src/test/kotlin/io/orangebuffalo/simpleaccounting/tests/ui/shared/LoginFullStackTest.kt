package io.orangebuffalo.simpleaccounting.tests.ui.shared

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.business.users.LoginStatistics
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.openLoginPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.shouldBeDashboardPage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.findById
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.newFixedThreadPoolContext

class LoginFullStackTest(
    @Autowired private val webTestClient: WebTestClient,
) : SaFullStackTestBase() {

    @BeforeEach
    fun setupCurrentTime() {
        mockCurrentTime(timeService)
    }

    @Test
    fun `should login as regular user and verify remember me cookie`(page: Page) {
        val loginPage = page.openLoginPage()
            .reportRendering("login.initial-state")
            .loginButton {
                shouldBeDisabled()
                shouldHaveLabelSatisfying { it.shouldBeEqualIgnoringCase("Login") }
            }
            .rememberMeCheckbox { shouldBeChecked() }
            .loginInput { fill(preconditions.fry.userName) }
            .loginButton { shouldBeDisabled() }
            .passwordInput { fill(preconditions.fry.passwordHash) }
            .loginButton { shouldBeEnabled() }
            .reportRendering("login.filled-state")

        page.withBlockedApiResponse(
            "**",
            initiator = {
                loginPage.loginButton { click() }
            },
            blockedRequestSpec = {
                loginPage.loginInput.shouldBeDisabled()
                loginPage.passwordInput.shouldBeDisabled()
                loginPage.rememberMeCheckbox.shouldBeDisabled()
                loginPage.loginButton.shouldBeDisabled()
                loginPage.loginButton.shouldHaveLabelSatisfying { it.shouldBeEmpty() }
                loginPage.reportRendering("login.loading-state")
            }
        )

        page.shouldBeDashboardPage()

        val cookies = page.context().cookies()
        val refreshCookie = cookies.find { it.name == "refreshToken" }
        refreshCookie.shouldNotBeNull()
        refreshCookie.value.shouldNotBeNull()
        refreshCookie.httpOnly.shouldBe(true)
        refreshCookie.path.shouldBe("/api/auth/token")

        // we do not set the browser time, so just verify that expiry is roughly correct
        val expectedExpires = (Instant.now().toEpochMilli() / 1000 + Duration.ofDays(30).seconds).toDouble()
        refreshCookie.expires.shouldBeIn(expectedExpires - 20..expectedExpires + 20)

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(0)
            temporaryLockExpirationTime.shouldBeNull()
        }

        aggregateTemplate.findSingle<RefreshToken>().should { token ->
            token.userId.shouldBe(preconditions.fry.id)
            token.token.shouldBe(refreshCookie.value)
            token.expirationTime.shouldBe(MOCK_TIME.plus(Duration.ofDays(30)))
        }
    }

    @Test
    fun `should login as admin user`(page: Page) {
        page.openLoginPage()
            .loginButton { shouldBeDisabled() }
            .rememberMeCheckbox { shouldBeChecked() }
            .loginInput { fill(preconditions.farnsworth.userName) }
            .loginButton { shouldBeDisabled() }
            .passwordInput { fill(preconditions.farnsworth.passwordHash) }
            .loginButton {
                shouldBeEnabled()
                click()
            }
        page.shouldBeUsersOverviewPage()
    }

    @Test
    fun `should show bad credentials error and update login statistics`(page: Page) {
        mockWrongPassword()

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill("wrongpassword") }
            .loginButton { click() }
            .shouldHaveErrorMessage("Login attempt failed. Please make sure login and password is correct")
            .reportRendering("login.error-state")
            .loginInput { shouldBeEnabled() }
            .passwordInput { shouldBeEnabled() }
            .loginButton { shouldBeEnabled() }
            .rememberMeCheckbox { shouldBeEnabled() }

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(1)
            temporaryLockExpirationTime.shouldBeNull()
        }

        aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
    }

    @Test
    fun `should lock account after multiple failed attempts`(page: Page) {
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = null
        }
        mockWrongPassword()

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill("wrongpassword") }
            .loginButton { click() }
            .shouldHaveErrorMessageMatching("Account is temporary locked\\. It will be unlocked in 0:\\d\\d")

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(6)
            temporaryLockExpirationTime.shouldBe(MOCK_TIME.plusSeconds(60))
        }
    }

    @Test
    fun `should allow login after lock expiration and reset statistics`(page: Page) {
        setupFryLoginStatistics {
            failedAttemptsCount = 6
            // Lock expired 1 second ago
            temporaryLockExpirationTime = MOCK_TIME.minusSeconds(1)
        }

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill(preconditions.fry.passwordHash) }
            .loginButton { click() }

        page.shouldBeDashboardPage()

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(0)
            temporaryLockExpirationTime.shouldBeNull()
        }
    }

    @Test
    fun `should prevent login when account is currently locked`(page: Page) {
        setupFryLoginStatistics {
            failedAttemptsCount = 6
            temporaryLockExpirationTime = MOCK_TIME.plusSeconds(300)
        }

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            // correct password
            .passwordInput { fill(preconditions.fry.passwordHash) }
            .loginButton { click() }
            .shouldHaveErrorMessageMatching("Account is temporary locked\\. It will be unlocked in \\d:\\d\\d")

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(6)
            temporaryLockExpirationTime.shouldBe(MOCK_TIME.plusSeconds(300))
        }
    }

    @Test
    fun `should show progressive locking for repeated failures`(page: Page) {
        setupFryLoginStatistics {
            failedAttemptsCount = 7
            temporaryLockExpirationTime = MOCK_TIME.minusSeconds(1)
        }
        mockWrongPassword()

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill("wrongpassword") }
            .loginButton { click() }
            .shouldHaveErrorMessageMatching("Account is temporary locked\\. It will be unlocked in \\d:\\d\\d")

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(8)
            temporaryLockExpirationTime.shouldBe(MOCK_TIME.plusSeconds(135))
        }
    }

    @Test
    fun `should forbid login for not activated users`(page: Page) {
        whenever(passwordEncoder.matches(preconditions.scruffy.passwordHash, preconditions.scruffy.passwordHash)) doReturn true

        page.openLoginPage()
            .loginInput { fill(preconditions.scruffy.userName) }
            .passwordInput { fill(preconditions.scruffy.passwordHash) }
            .loginButton { click() }
            .shouldHaveErrorMessage("Your account is not yet activated. Please use the token shared with you by the administrators. Contact them if you need to reset the token")
            .reportRendering("login.not-activated-error")
    }

    @Test
    fun `should show login not available error when authentication queue is overloaded`(page: Page) {
        mockWrongPassword()

        val loginPage = page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill("wrongpassword") }

        // Load the authentication queue with parallel login attempts using REST API
        loadAuthenticationQueue()

        // Attempt login via UI while queue is loaded
        loginPage
            .loginButton { click() }
            .shouldHaveErrorMessage("Looks like your account is under attack!")
    }

    private fun loadAuthenticationQueue() {
        // Create parallel authentication requests to load the queue
        // Using direct API calls to efficiently create background load
        val requests = generateSequence(1) { if (it < 10) it + 1 else null }
            .map {
                kotlinx.coroutines.GlobalScope.async(kotlinx.coroutines.newFixedThreadPoolContext(10, "parallelLogins")) {
                    try {
                        webTestClient.post()
                            .uri("/api/auth/login")
                            .bodyValue(mapOf(
                                "userName" to preconditions.fry.userName,
                                "password" to "wrongpassword",
                                "rememberMe" to false
                            ))
                            .exchange()
                    } catch (e: Exception) {
                        // Ignore exceptions, we just want to load the queue
                        null
                    }
                }
            }
            .toList()

        // Give requests time to start processing before the UI attempt
        Thread.sleep(100)
    }

    private fun mockWrongPassword() {
        whenever(passwordEncoder.matches("wrongpassword", preconditions.fry.passwordHash)) doReturn false
    }

    private fun assertFryLoginStatistics(spec: LoginStatistics.() -> Unit) {
        val fry = aggregateTemplate.findById<PlatformUser>(preconditions.fry.id!!)!!
        fry.loginStatistics.spec()
    }

    private fun setupFryLoginStatistics(spec: LoginStatistics.() -> Unit) {
        val fry = aggregateTemplate.findById<PlatformUser>(preconditions.fry.id!!)!!
        fry.loginStatistics.spec()
        aggregateTemplate.save(fry)
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
            val farnsworth = farnsworth()
            val scruffy = scruffy()
        }
    }
}
