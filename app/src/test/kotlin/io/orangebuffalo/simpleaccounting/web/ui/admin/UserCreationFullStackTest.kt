package io.orangebuffalo.simpleaccounting.web.ui.admin

import com.microsoft.playwright.Page
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.CreateUserPage
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.shouldBeCreateUserPage
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.shouldBeEditUserPage
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate

@SimpleAccountingFullStackTest
class UserCreationFullStackTest(
    @Autowired private val entitiesTemplate: JdbcAggregateTemplate,
    preconditionsFactory: PreconditionsFactory,
) {

    @Test
    fun `should create a new user`(page: Page) {
        setupPreconditionsAndNavigateToCreatePage(page)
            .userName { input.fill("userX") }
            .activationStatus { shouldNotBeVisible() }
            .saveButton { click() }
            .shouldHaveNotifications {
                success("User userX has been successfully saved")
            }

        page.shouldBeEditUserPage()
            .activationStatus { shouldBeVisible() }

        entitiesTemplate.findAll<PlatformUser>()
            .shouldWithClue("Expected exactly one user created (and one pre-seeded with preconditions)") {
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
                withClue("By default, regular user should be pre-selected") {
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

        entitiesTemplate.findAll<PlatformUser>()
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

        entitiesTemplate.findAll<PlatformUser>()
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
        page.loginAs(preconditions.farnsworth)
        page.shouldHaveSideMenu().clickUsersOverview()
        page.shouldBeUsersOverviewPage()
            .createUserButton.click()
        return page.shouldBeCreateUserPage()
    }

    private val preconditions by preconditionsFactory {
        object {
            val farnsworth = platformUser(
                userName = "admin",
                passwordHash = "admin",
                isAdmin = true,
            )
        }
    }
}
