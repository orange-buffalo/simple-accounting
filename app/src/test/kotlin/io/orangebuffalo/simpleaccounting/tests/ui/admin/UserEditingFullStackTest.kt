package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldNotBe
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.EditUserPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.shouldBeEditUserPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate

@SimpleAccountingFullStackTest
class UserEditingFullStackTest(
    @Autowired private val entitiesTemplate: JdbcAggregateTemplate,
    @Autowired private val timeService: TimeService,
    private val preconditionsFactory: PreconditionsFactory,
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

    @Test
    fun `should render activated status`(page: Page) {
        setupPreconditionsAndNavigateToEditPage(page)
            .activationStatus {
                shouldBeVisible()
                input { shouldBeActivated() }
            }
    }

    @Test
    fun `should retrieve valid activation token`(page: Page) {
        mockCurrentTime(timeService)
        page.mockCurrentTime()
        val data = preconditionsFactory.setup {
            object {
                val farnsworth = farnsworth()
                val user = platformUser(
                    userName = "user",
                    activated = false,
                )

                init {
                    userActivationToken(
                        user = user,
                        token = "token-value",
                        expiresAt = MOCK_TIME.plusSeconds(1),
                    )
                }
            }
        }
        navigateToEditPage(page, data.farnsworth, data.user)
            .activationStatus {
                shouldBeVisible()
                input {
                    shouldBeNotActivated("token-value")
                }
            }

        entitiesTemplate.findAll<UserActivationToken>()
            .map { it.token }
            .shouldWithClue("Should keep valid token intact") {
                shouldContainExactly("token-value")
            }
    }

    @Test
    fun `should recreate activation token if expired`(page: Page) {
        mockCurrentTime(timeService)
        page.mockCurrentTime()
        val data = preconditionsFactory.setup {
            object {
                val farnsworth = farnsworth()
                val user = platformUser(
                    userName = "user",
                    activated = false,
                )

                init {
                    userActivationToken(
                        user = user,
                        token = "token-value",
                        expiresAt = MOCK_TIME.minusSeconds(1),
                    )
                }
            }
        }
        navigateToEditPage(page, data.farnsworth, data.user)
            .activationStatus {
                shouldBeVisible()
                input {
                    val newToken = shouldEventually("Should recreate token") {
                        entitiesTemplate.findAll<UserActivationToken>()
                            .map { it.token }
                            .shouldBeSingle()
                            .shouldNotBe("token-value")
                    }
                    shouldBeNotActivated(newToken)
                }
            }
    }

    @Test
    fun `should create activation token if not exists`(page: Page) {
        mockCurrentTime(timeService)
        page.mockCurrentTime()
        val data = preconditionsFactory.setup {
            object {
                val farnsworth = farnsworth()
                val user = platformUser(
                    userName = "user",
                    activated = false,
                )
            }
        }
        navigateToEditPage(page, data.farnsworth, data.user)
            .activationStatus {
                shouldBeVisible()
                input {
                    val newToken = shouldEventually("Should recreate token") {
                        entitiesTemplate.findAll<UserActivationToken>()
                            .map { it.token }
                            .shouldBeSingle()
                    }
                    shouldBeNotActivated(newToken)
                }
            }
    }

    private fun setupPreconditionsAndNavigateToEditPage(page: Page): EditUserPage =
        navigateToEditPage(page = page, admin = preconditions.farnsworth, userUnderEdit = preconditions.fry)

    private fun navigateToEditPage(page: Page, admin: PlatformUser, userUnderEdit: PlatformUser): EditUserPage {
        page.loginAs(admin)
        page.shouldHaveSideMenu().clickUsersOverview()
        page.shouldBeUsersOverviewPage()
            .pageItems {
                finishLoadingWhenTimeMocked()
                val userUnderEditItem = shouldHaveItemSatisfying {
                    it.title == userUnderEdit.userName
                }
                userUnderEditItem.executeAction("Edit")
            }
        return page.shouldBeEditUserPage()
    }
}
