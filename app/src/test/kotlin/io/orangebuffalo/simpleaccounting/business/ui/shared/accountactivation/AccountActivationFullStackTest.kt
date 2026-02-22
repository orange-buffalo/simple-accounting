package io.orangebuffalo.simpleaccounting.business.ui.shared.accountactivation

import com.microsoft.playwright.Page
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.accountactivation.AccountActivationPage.Companion.openAccountActivationPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.user.accountsetup.AccountSetupPage.Companion.shouldBeAccountSetupPage
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldHaveNotifications
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class AccountActivationFullStackTest(
    @param:Autowired private val timeServiceSpy: TimeService,
) : SaFullStackTestBase() {
    private val preconditions by lazyPreconditions {
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
        page.openAccountActivationPage("xxx") {
            userMessage {
                shouldBeError("Provided token is invalid or expired. Please request a new one.")
            }
            loginButton { shouldBeHidden() }
            form { shouldNotBeVisible() }
        }
    }

    @Test
    fun `should provide feedback if token expired during activation`(page: Page) {
        page.openAccountActivationPage(preconditions.token.token) {
            userMessage {
                shouldBeRegular("Please provide your new password. You will then need to login using your username and new password.")
            }
            form { shouldBeVisible() }
            loginButton { shouldBeHidden() }

            // now, token is expired - to simulate token expiration during activation
            whenever(timeServiceSpy.currentTime()) doReturn preconditions.token.expiresAt.plusSeconds(1)

            form {
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
            userMessage {
                shouldBeError("Provided token is invalid or expired. Please request a new one.")
            }
            loginButton { shouldBeHidden() }
        }
    }

    @Test
    fun `should validate user input`(page: Page) {
        page.openAccountActivationPage(preconditions.token.token) {
            form {
                withHint("Should validate password match when confirmation is not provided") {
                    newPassword.input.fill("abc")
                    activateAccountButton.click()
                    page.shouldHaveNotifications {
                        validationFailed()
                    }
                    shouldBeVisible()
                    newPassword.shouldNotHaveValidationErrors()
                    newPasswordConfirmation.shouldHaveValidationError("Passwords do not match")
                }

                withHint("Should validate password match when confirmation is provided") {
                    newPassword.input.fill("abc")
                    newPasswordConfirmation.input.fill("def")
                    activateAccountButton.click()
                    page.shouldHaveNotifications {
                        validationFailed()
                    }
                    shouldBeVisible()
                    newPassword.shouldNotHaveValidationErrors()
                    newPasswordConfirmation.shouldHaveValidationError("Passwords do not match")
                }

                withHint("Should prohibit empty password") {
                    newPassword.input.fill("")
                    newPasswordConfirmation.input.fill("")
                    activateAccountButton.click()
                    page.shouldHaveNotifications {
                        validationFailed()
                    }
                    shouldBeVisible()
                    newPassword.shouldHaveValidationError("This value is required and should not be blank")
                    newPasswordConfirmation.shouldNotHaveValidationErrors()
                }

                withHint("Should prohibit blank password") {
                    newPassword.input.fill("  ")
                    newPasswordConfirmation.input.fill("  ")
                    activateAccountButton.click()
                    page.shouldHaveNotifications {
                        validationFailed()
                    }
                    shouldBeVisible()
                    newPassword.shouldHaveValidationError("This value is required and should not be blank")
                    newPasswordConfirmation.shouldNotHaveValidationErrors()
                }

                withHint("Should prohibit too long passwords") {
                    newPassword.input.fill("a".repeat(101))
                    newPasswordConfirmation.input.fill("a".repeat(101))
                    activateAccountButton.click()
                    page.shouldHaveNotifications {
                        validationFailed()
                    }
                    shouldBeVisible()
                    newPassword.shouldHaveValidationError("The length of this value should be no longer than 100 characters")
                    newPasswordConfirmation.shouldNotHaveValidationErrors()
                }
            }
        }
    }

    @Test
    fun `should activate user account`(page: Page) {
        page.openAccountActivationPage(preconditions.token.token) {
            form {
                newPassword.input.fill("qwerty")
                newPasswordConfirmation.input.fill("qwerty")
                activateAccountButton.click()
                shouldNotBeVisible()
            }
            userMessage {
                shouldBeSuccess("Account has been activated. You can now login using your credentials.")
            }
            loginButton {
                shouldBeVisible()
                click()
            }
        }

        // reset time to generate valid JWT token on login
        whenever(timeServiceSpy.currentTime()) doReturn Instant.now()

        withHint("Should update database with activated user") {
            aggregateTemplate.findSingle<PlatformUser>().should {
                it.activated.shouldBeTrue()
                // test password encoder saves value as is
                it.passwordHash.shouldBe("qwerty")
            }
        }

        withHint("Should login after activation") {
            page.shouldBeLoginPage {
                loginInput {
                    fill(preconditions.user.userName)
                }
                passwordInput {
                    fill("qwerty")
                }
                loginButton {
                    click()
                }
            }

            page.shouldBeAccountSetupPage()
        }
    }
}
