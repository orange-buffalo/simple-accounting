package io.orangebuffalo.simpleaccounting.web.ui.shared

import com.microsoft.playwright.Page
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.openAccountActivationPage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.web.ui.user.pages.shouldBeWorkspaceSetupPage
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import java.time.Instant

@SimpleAccountingFullStackTest
class AccountActivationFullStackTest(
    @Autowired private val timeServiceSpy: TimeService,
    @Autowired private val aggregateTemplate: JdbcAggregateTemplate,
    preconditionsFactory: PreconditionsFactory,
) {
    private val preconditions by preconditionsFactory {
        object {
            val user = platformUser(
                activated = false,
                userName = "new-user",
            )
            val token = userActivationToken(
                expiresAt = Instant.ofEpochMilli(1577800922000),
                user = user,
            )

            init {
                whenever(timeServiceSpy.currentTime()) doReturn token.expiresAt
            }
        }
    }

    @Test
    fun `should provide feedback if token is not known`(page: Page) {
        page.openAccountActivationPage("xxx")
            .userMessage {
                shouldBeError("Provided token is invalid or expired. Please request a new one.")
            }
            .loginButton { shouldNotBeVisible() }
            .form { shouldNotBeVisible() }
    }

    @Test
    fun `should provide feedback if token expired during activation`(page: Page) {
        page.openAccountActivationPage(preconditions.token.token)
            .userMessage {
                shouldBeRegular("Please provide your new password. You will then need to login using your username and new password.")
            }
            .form { shouldBeVisible() }
            .loginButton { shouldNotBeVisible() }
            .then {
                // now, token is expired - to simulate token expiration during activation
                whenever(timeServiceSpy.currentTime()) doReturn preconditions.token.expiresAt.plusSeconds(1)
            }
            .form {
                newPassword {
                    input { fill("qwerty") }
                }
                newPasswordConfirmation {
                    input { fill("qwerty") }
                }
                activateAccountButton {
                    click()
                }
                shouldNotBeVisible()
            }
            .userMessage {
                shouldBeError("Provided token is invalid or expired. Please request a new one.")
            }
            .loginButton { shouldNotBeVisible() }
    }

    @Test
    fun `should validate user input`(page: Page) {
        page.openAccountActivationPage(preconditions.token.token).form {
            withClue("Should validate password match when confirmation is not provided") {
                newPassword.input.fill("abc")
                activateAccountButton.click()
                shouldBeVisible()
                newPassword.shouldNotHaveValidationErrors()
                newPasswordConfirmation.shouldHaveValidationError("Passwords do not match")
            }

            withClue("Should validate password match when confirmation is provided") {
                newPassword.input.fill("abc")
                newPasswordConfirmation.input.fill("def")
                activateAccountButton.click()
                shouldBeVisible()
                newPassword.shouldNotHaveValidationErrors()
                newPasswordConfirmation.shouldHaveValidationError("Passwords do not match")
            }

            withClue("Should prohibit empty password") {
                newPassword.input.fill("")
                newPasswordConfirmation.input.fill("")
                activateAccountButton.click()
                shouldBeVisible()
                newPassword.shouldHaveValidationError("This value is required and should not be blank")
                newPasswordConfirmation.shouldNotHaveValidationErrors()
            }

            withClue("Should prohibit blank password") {
                newPassword.input.fill("  ")
                newPasswordConfirmation.input.fill("  ")
                activateAccountButton.click()
                shouldBeVisible()
                newPassword.shouldHaveValidationError("This value is required and should not be blank")
                newPasswordConfirmation.shouldNotHaveValidationErrors()
            }

            withClue("Should prohibit too long passwords") {
                newPassword.input.fill("a".repeat(101))
                newPasswordConfirmation.input.fill("a".repeat(101))
                activateAccountButton.click()
                shouldBeVisible()
                newPassword.shouldHaveValidationError("The length of this value should be no longer than 100 characters")
                newPasswordConfirmation.shouldNotHaveValidationErrors()
            }
        }
    }

    @Test
    fun `should activate user account`(page: Page) {
        page.openAccountActivationPage(preconditions.token.token)
            .form {
                newPassword.input.fill("qwerty")
                newPasswordConfirmation.input.fill("qwerty")
                activateAccountButton.click()
                shouldNotBeVisible()
            }
            .userMessage {
                shouldBeSuccess("Account has been activated. You can now login using your credentials.")
            }
            .loginButton {
                shouldBeVisible()
                click()
            }

        val loginPage = page.shouldBeLoginPage()

        // reset time to generate valid JWT token on login
        Mockito.reset(timeServiceSpy)

        withClue("Should update database with activated user") {
            aggregateTemplate.findSingle<PlatformUser>().should {
                it.activated.shouldBeTrue()
                // test password encoder saves value as is
                it.passwordHash.shouldBe("qwerty")
            }
        }

        withClue("Should login after activation") {
            loginPage
                .loginInput {
                    fill(preconditions.user.userName)
                }
                .passwordInput {
                    fill("qwerty")
                }
                .loginButton {
                    click()
                }

            page.shouldBeWorkspaceSetupPage()
        }
    }
}
