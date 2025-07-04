package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.*
import org.junit.jupiter.api.Test

class UserCreationFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should create a new user`(page: Page) {
        setupPreconditionsAndNavigateToCreatePage(page)
            .userName { input.fill("userX") }
            .activationStatus { shouldBeHidden() }
            .saveButton { click() }
            .shouldHaveNotifications {
                success("User userX has been successfully saved")
            }

        page.shouldBeEditUserPage()
            .activationStatus { shouldBeVisible() }

        aggregateTemplate.findAll<PlatformUser>()
            .shouldWithHint("Expected exactly one user created (and one pre-seeded with preconditions)") {
                shouldHaveSize(2)
            }
            .filter { it.userName == "userX" }
            .shouldBeSingle()
            .shouldBeEntityWithFields(
                PlatformUser(
                    userName = "userX",
                    activated = false,
                    passwordHash = "",
                    isAdmin = false,
                ),
                ignoredProperties = arrayOf(PlatformUser::passwordHash),
            )
    }

    @Test
    fun `should support user role`(page: Page) {
        setupPreconditionsAndNavigateToCreatePage(page)
            .role {
                withHint("By default, regular user should be pre-selected") {
                    input.shouldHaveSelectedValue("User")
                }
                input.shouldHaveOptions("User", "Admin user")
                input.selectOption("User")
            }
            .userName { input.fill("user") }
            .saveButton { click() }
            .shouldHaveNotifications {
                success()
            }

        page.shouldBeEditUserPage().cancelButton { click() }
        val overviewPage = page.shouldBeUsersOverviewPage()

        aggregateTemplate.findAll<PlatformUser>()
            .filter { it.userName == "user" }
            .shouldBeSingle()
            .should {
                it.isAdmin.shouldBeFalse()
            }

        overviewPage.createUserButton.click()

        page.shouldBeCreateUserPage()
            .userName { input.fill("new-admin") }
            .role {
                input.selectOption("Admin user")
            }
            .saveButton { click() }
            .shouldHaveNotifications {
                success()
            }

        aggregateTemplate.findAll<PlatformUser>()
            .filter { it.userName == "new-admin" }
            .shouldBeSingle()
            .should {
                it.isAdmin.shouldBeTrue()
            }
    }

    @Test
    fun `should navigate to overview on creation cancel`(page: Page) {
        setupPreconditionsAndNavigateToCreatePage(page)
            .cancelButton { click() }

        page.shouldBeUsersOverviewPage()
    }

    @Test
    fun `should validate input`(page: Page) {
        val createUserPage = setupPreconditionsAndNavigateToCreatePage(page)

        createUserPage.saveButton { click() }
            .shouldHaveNotifications {
                validationFailed()
            }
            .userName {
                shouldHaveValidationError("This value is required and should not be blank")
            }
            .role {
                shouldNotHaveValidationErrors()
            }

        createUserPage.userName { input.fill("x".repeat(256)) }
            .saveButton { click() }
            .shouldHaveNotifications {
                validationFailed()
            }
            .userName {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }

        createUserPage.userName { input.fill("x".repeat(255)) }
            .saveButton { click() }
            .shouldHaveNotifications {
                success()
            }
    }

    @Test
    fun `should validate user name uniqueness`(page: Page) {
        setupPreconditionsAndNavigateToCreatePage(page)
            .userName { input.fill("admin") }
            .saveButton { click() }
            .userName { shouldHaveValidationError("User with username \"admin\" already exists") }
            .shouldHaveNotifications {
                validationFailed()
            }
    }

    private fun setupPreconditionsAndNavigateToCreatePage(page: Page): CreateUserPage {
        page.authenticateViaCookie(preconditions.farnsworth)
        page.openUsersOverviewPage().createUserButton.click()
        return page.shouldBeCreateUserPage()
    }

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = platformUser(
                userName = "admin",
                passwordHash = "admin",
                isAdmin = true,
            )
        }
    }
}
