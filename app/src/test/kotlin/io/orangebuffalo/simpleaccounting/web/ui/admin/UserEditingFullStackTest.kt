package io.orangebuffalo.simpleaccounting.web.ui.admin

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.*
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate

@SimpleAccountingFullStackTest
class UserEditingFullStackTest(
    @Autowired private val entitiesTemplate: JdbcAggregateTemplate,
    preconditionsFactory: PreconditionsFactory,
) {
    private val preconditions by preconditionsFactory {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
        }
    }

    @Test
    fun `should update user`(page: Page) {
        setupPreconditionsAndNavigateToEditPage(page)
            .userName {
                input.shouldHaveValue(preconditions.fry.userName)
            }
            .role {
                input.shouldBeDisabled()
            }
            .userName {
                input.fill("fryX")
            }
            .saveButton { click() }
            .shouldHaveNotifications {
                success("User fryX has been successfully saved")
            }

        page.shouldBeUsersOverviewPage()

        val actualUsers = entitiesTemplate.findAll<PlatformUser>()
        actualUsers.map { user -> user.userName }
            .shouldWithClue("Should update Fry user") {
                shouldContainExactlyInAnyOrder("fryX", preconditions.farnsworth.userName)
            }
    }

    @Test
    fun `should navigate to overview on update cancel`(page: Page) {
        setupPreconditionsAndNavigateToEditPage(page)
            .cancelButton { click() }

        page.shouldBeUsersOverviewPage()
    }

    @Test
    fun `should validate input`(page: Page) {
        val editUserPage = setupPreconditionsAndNavigateToEditPage(page)

        editUserPage
            .userName {
                // wait for page data to load
                input.shouldHaveValue(preconditions.fry.userName)
                input.fill("")
            }
            .saveButton { click() }
            .shouldHaveNotifications {
                validationFailed()
            }
            .userName {
                shouldHaveValidationError("This value is required and should not be blank")
            }
            .role {
                shouldNotHaveValidationErrors()
            }

        editUserPage
            .userName { input.fill("x".repeat(256)) }
            .saveButton { click() }
            .shouldHaveNotifications {
                validationFailed()
            }
            .userName {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }

        editUserPage.userName { input.fill("x".repeat(255)) }
            .saveButton { click() }
            .shouldHaveNotifications {
                success()
            }
    }

    @Test
    fun `should validate user name uniqueness`(page: Page) {
        setupPreconditionsAndNavigateToEditPage(page)
            .userName {
                // wait for page data to load
                input.shouldHaveValue(preconditions.fry.userName)
                input.fill(preconditions.farnsworth.userName)
            }
            .saveButton { click() }
            .userName { shouldHaveValidationError("User with username \"${preconditions.farnsworth.userName}\" already exists") }
            .shouldHaveNotifications {
                validationFailed()
            }
    }

    @Test
    fun `should not fail with validation if user name is not changed`(page: Page) {
        setupPreconditionsAndNavigateToEditPage(page)
            .userName {
                // wait for page data to load
                input.shouldHaveValue(preconditions.fry.userName)
            }
            .saveButton { click() }
            .shouldHaveNotifications {
                success()
            }
    }

    private fun setupPreconditionsAndNavigateToEditPage(page: Page): EditUserPage {
        page.loginAs(preconditions.farnsworth)
        page.shouldHaveSideMenu().clickUsersOverview()
        page.shouldBeUsersOverviewPage()
            .pageItems {
                val fryItem = shouldHaveItemSatisfying {
                    it.title == preconditions.fry.userName
                }
                fryItem.executeAction("Edit")
            }
        return page.shouldBeEditUserPage()
    }
}
