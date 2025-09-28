package io.orangebuffalo.simpleaccounting.tests.ui.shared

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.business.users.LoginStatistics
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersRepository
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.openLoginPage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.shouldBeDashboardPage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.Duration

private val MOCK_TIME = Instant.ofEpochMilli(424242)

class LoginFullStackTest(
    @Autowired private val transactionTemplate: TransactionTemplate,
    @Autowired private val platformUsersRepository: PlatformUsersRepository,
) : SaFullStackTestBase() {

    @BeforeEach
    fun setupCurrentTime() {
        whenever(timeService.currentTime()) doReturn MOCK_TIME
    }

    @Test
    fun `should login as regular user and verify remember me cookie`(page: Page) {
        page.openLoginPage()
            .loginButton { shouldBeDisabled() }
            .rememberMeCheckbox { shouldBeChecked() }
            .loginInput { fill(preconditions.fry.userName) }
            .loginButton { shouldBeDisabled() }
            .passwordInput { fill(preconditions.fry.passwordHash) }
            .loginButton {
                shouldBeEnabled()
                click()
            }
        page.shouldBeDashboardPage()

        // Verify remember me cookie was set with proper security attributes and expiration
        val cookies = page.context().cookies()
        val refreshCookie = cookies.find { it.name == "refreshToken" }
        refreshCookie.shouldNotBeNull()
        refreshCookie.value.shouldNotBeNull()
        refreshCookie.httpOnly.shouldBe(true)
        refreshCookie.path.shouldBe("/api/auth/token")
        
        // Verify cookie expiration time is set to 30 days from now (with some tolerance) 
        val expectedExpiry = (timeService.currentTime().toEpochMilli() / 1000) + Duration.ofDays(30).seconds
        val actualExpiry = refreshCookie.expires
        val toleranceInSeconds = 60.0 // Allow 60 seconds tolerance
        Math.abs(actualExpiry - expectedExpiry).shouldBeLessThan(toleranceInSeconds)

        // Verify login statistics were reset
        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(0)
            temporaryLockExpirationTime.shouldBeNull()
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
        // Override the default password encoder mocking to actually fail bad credentials
        whenever(passwordEncoder.matches("wrongpassword", preconditions.fry.passwordHash)) doReturn false

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill("wrongpassword") }
            .loginButton { click() }

        page.shouldBeLoginPage()
            .shouldHaveErrorMessage("Login attempt failed. Please make sure login and password is correct")

        // Verify login statistics were updated
        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(1)
            temporaryLockExpirationTime.shouldBeNull()
        }
    }

    @Test
    fun `should lock account after multiple failed attempts`(page: Page) {
        // Setup user that should be locked on next failure
        setupFryLoginStatistics {
            failedAttemptsCount = 5
            temporaryLockExpirationTime = null
        }
        
        // Mock password to fail which should trigger locking
        whenever(passwordEncoder.matches("wrongpassword", preconditions.fry.passwordHash)) doReturn false

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill("wrongpassword") }
            .loginButton { click() }

        page.shouldBeLoginPage()
            .shouldHaveErrorMessage("Account is temporary locked. It will be unlocked in 1 min")

        // Verify account is now locked with updated statistics - exact time based on mock time
        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(6)
            temporaryLockExpirationTime.shouldNotBeNull()
            temporaryLockExpirationTime.shouldBe(MOCK_TIME.plusSeconds(60)) // 1 minute lock for 6th attempt
        }
    }

    @Test
    fun `should allow login after lock expiration and reset statistics`(page: Page) {
        // Set up user with expired lock
        setupFryLoginStatistics {
            failedAttemptsCount = 6
            temporaryLockExpirationTime = MOCK_TIME.minusSeconds(1) // Lock expired 1 second ago
        }

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill(preconditions.fry.passwordHash) }
            .loginButton { click() }

        page.shouldBeDashboardPage()

        // Verify login statistics were reset
        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(0)
            temporaryLockExpirationTime.shouldBeNull()
        }
    }

    @Test
    fun `should prevent login when account is currently locked`(page: Page) {
        // Set up user with active lock
        setupFryLoginStatistics {
            failedAttemptsCount = 6
            temporaryLockExpirationTime = MOCK_TIME.plusSeconds(300) // 5 minutes from now
        }

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill(preconditions.fry.passwordHash) } // Even correct password should fail
            .loginButton { click() }

        page.shouldBeLoginPage()
            .shouldHaveErrorMessage("Account is temporary locked. It will be unlocked in 5 min")

        // Verify lock is still in place (no additional attempts recorded)
        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(6)
            temporaryLockExpirationTime.shouldBe(MOCK_TIME.plusSeconds(300))
        }
    }

    @Test
    fun `should show progressive locking for repeated failures`(page: Page) {
        // Set up for a user with multiple previous failures that should get progressively longer lock
        setupFryLoginStatistics {
            failedAttemptsCount = 7
            temporaryLockExpirationTime = MOCK_TIME.minusSeconds(1) // Previous lock expired
        }
        
        // Mock password to fail
        whenever(passwordEncoder.matches("wrongpassword", preconditions.fry.passwordHash)) doReturn false

        page.openLoginPage()
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill("wrongpassword") }
            .loginButton { click() }

        page.shouldBeLoginPage()
            .shouldHaveErrorMessage("Account is temporary locked. It will be unlocked in 2 min 15 sec")

        // Verify login statistics show progressive locking
        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(8)
            temporaryLockExpirationTime.shouldNotBeNull()
            // The lock time should be progressively longer - 8th attempt gets 2 minutes 15 seconds (135 seconds)
            temporaryLockExpirationTime.shouldBe(MOCK_TIME.plusSeconds(135))
        }
    }

    @Test
    fun `should capture screenshots for UI verification`(page: Page) {
        // Screenshot 1: Initial loading state once UI is rendered
        val loginPage = page.openLoginPage()
        page.locator(".login-page").reportRendering("login.initial-state")

        // Screenshot 2: Loading state of the login button (while login API request is being processed)
        page.withBlockedApiResponse(
            "**/login",
            initiator = {
                loginPage
                    .loginInput { fill(preconditions.fry.userName) }
                    .passwordInput { fill(preconditions.fry.passwordHash) }
                    .loginButton { click() }
            },
            blockedRequestSpec = {
                loginPage.loginButton.shouldBeDisabled()
                page.locator(".login-page").reportRendering("login.loading-state")
            }
        )

        // After successful login, navigate back to get error state
        page.openLoginPage()

        // Screenshot 3: Error message after failed login attempt
        whenever(passwordEncoder.matches("wrongpassword", preconditions.fry.passwordHash)) doReturn false
        
        loginPage
            .loginInput { fill(preconditions.fry.userName) }
            .passwordInput { fill("wrongpassword") }
            .loginButton { click() }
        
        loginPage.shouldHaveErrorMessage("Login attempt failed. Please make sure login and password is correct")
        page.locator(".login-page").reportRendering("login.error-state")
    }

    private fun assertFryLoginStatistics(spec: LoginStatistics.() -> Unit) {
        transactionTemplate.execute {
            val fry = platformUsersRepository.findByUserName(preconditions.fry.userName)
                ?: throw IllegalStateException("Fry is not found?!")
            fry.loginStatistics.spec()
        }
    }

    private fun setupFryLoginStatistics(spec: LoginStatistics.() -> Unit) {
        transactionTemplate.execute {
            val fry = platformUsersRepository.findByUserName(preconditions.fry.userName)
                ?: throw IllegalStateException("Fry is not found?!")
            fry.loginStatistics.spec()
            platformUsersRepository.save(fry)
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
            val farnsworth = farnsworth()
        }
    }
}