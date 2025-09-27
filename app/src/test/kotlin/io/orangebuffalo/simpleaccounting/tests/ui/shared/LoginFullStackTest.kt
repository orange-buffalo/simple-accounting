package io.orangebuffalo.simpleaccounting.tests.ui.shared

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.microsoft.playwright.Page
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

        // Verify remember me cookie was set
        val cookies = page.context().cookies()
        val refreshCookie = cookies.find { it.name == "refreshToken" }
        assertThat(refreshCookie).isNotNull()
        assertThat(refreshCookie!!.value).isNotNull()
        assertThat(refreshCookie.httpOnly).isEqualTo(true)
        assertThat(refreshCookie.path).isEqualTo("/api/auth/token")

        // Verify login statistics were reset
        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(0)
            assertThat(temporaryLockExpirationTime).isNull()
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

        // Wait and verify we're still on login page (indicates failure)
        page.waitForTimeout(3000.0)
        page.shouldBeLoginPage()

        // Verify login statistics were updated
        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(1)
            assertThat(temporaryLockExpirationTime).isNull()
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

        // Verify we stay on login page (indicates login failed)
        page.waitForTimeout(3000.0)
        page.shouldBeLoginPage()

        // Verify account is now locked with updated statistics
        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(6)
            assertThat(temporaryLockExpirationTime).isNotNull()
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
            assertThat(failedAttemptsCount).isEqualTo(0)
            assertThat(temporaryLockExpirationTime).isNull()
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

        // Verify we stay on login page (locked out)
        page.waitForTimeout(3000.0)
        page.shouldBeLoginPage()

        // Verify lock is still in place (no additional attempts recorded)
        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(6)
            assertThat(temporaryLockExpirationTime).isEqualTo(MOCK_TIME.plusSeconds(300))
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

        // Verify we stay on login page
        page.waitForTimeout(3000.0)
        page.shouldBeLoginPage()

        // Verify login statistics show progressive locking
        assertFryLoginStatistics {
            assertThat(failedAttemptsCount).isEqualTo(8)
            assertThat(temporaryLockExpirationTime).isNotNull()
            // The lock time should be longer than the initial 60 seconds due to progressive increase
            val lockDurationMs = temporaryLockExpirationTime!!.toEpochMilli() - MOCK_TIME.toEpochMilli()
            assertThat(lockDurationMs).isGreaterThan(60000L) // More than 1 minute
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