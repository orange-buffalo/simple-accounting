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
        val loginPage = page.openLoginPage()
            .reportRendering("login.initial-state")
            .loginButton { shouldBeDisabled() }
            .rememberMeCheckbox { shouldBeChecked() }
            .loginInput { fill(preconditions.fry.userName) }
            .loginButton { shouldBeDisabled() }
            .passwordInput { fill(preconditions.fry.passwordHash) }
            .loginButton { shouldBeEnabled() }

        page.withBlockedApiResponse(
            "**/login",
            initiator = {
                loginPage.loginButton { click() }
            },
            blockedRequestSpec = {
                loginPage.loginButton.shouldBeDisabled()
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
        
        val expectedExpiry = (MOCK_TIME.toEpochMilli() / 1000) + Duration.ofDays(30).seconds
        refreshCookie.expires.shouldBe(expectedExpiry)

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
            .reportRendering("login.error-state")

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

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(6)
            temporaryLockExpirationTime.shouldNotBeNull()
            temporaryLockExpirationTime.shouldBe(MOCK_TIME.plusSeconds(60))
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

        assertFryLoginStatistics {
            failedAttemptsCount.shouldBe(8)
            temporaryLockExpirationTime.shouldNotBeNull()
            temporaryLockExpirationTime.shouldBe(MOCK_TIME.plusSeconds(135))
        }
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