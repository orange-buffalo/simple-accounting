package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldNotBe
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.EditUserPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.openUsersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.shouldBeEditUserPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.shouldBeUsersOverviewPage
import org.junit.jupiter.api.Test

class UserEditingFullStackTest : SaFullStackTestBase() {
    private val preconditions by lazyPreconditions {
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

        val actualUsers = aggregateTemplate.findAll<PlatformUser>()
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
        page.shouldBeEditUserPage {
            activationStatus {
                shouldBeVisible()
                input { shouldBeActivated() }
            }
        }
    }

    @Test
    fun `should retrieve valid activation token`(page: Page) {
        mockCurrentTime(timeService)
        page.mockCurrentTime()
        val data = preconditions {
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
        page.shouldBeEditUserPage {
            activationStatus {
                shouldBeVisible()
                input {
                    shouldBeNotActivated("token-value")
                }
            }
        }

        aggregateTemplate.findAll<UserActivationToken>()
            .map { it.token }
            .shouldWithClue("Should keep valid token intact") {
                shouldContainExactly("token-value")
            }
    }

    @Test
    fun `should recreate activation token if expired`(page: Page) {
        mockCurrentTime(timeService)
        page.mockCurrentTime()
        val data = preconditions {
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
        page.shouldBeEditUserPage {
            activationStatus {
                shouldBeVisible()
                input {
                    val newToken = shouldEventually("Should recreate token") {
                        aggregateTemplate.findAll<UserActivationToken>()
                            .map { it.token }
                            .shouldBeSingle()
                            .shouldNotBe("token-value")
                    }
                    shouldBeNotActivated(newToken)
                }
            }
        }
    }

    @Test
    fun `should create activation token if not exists`(page: Page) {
        mockCurrentTime(timeService)
        page.mockCurrentTime()
        val data = preconditions {
            object {
                val farnsworth = farnsworth()
                val user = platformUser(
                    userName = "user",
                    activated = false,
                )
            }
        }
        navigateToEditPage(page, data.farnsworth, data.user)
        page.shouldBeEditUserPage {
            activationStatus {
                shouldBeVisible()
                input {
                    val newToken = shouldEventually("Should recreate token") {
                        aggregateTemplate.findAll<UserActivationToken>()
                            .map { it.token }
                            .shouldBeSingle()
                    }
                    shouldBeNotActivated(newToken)
                }
            }
        }
    }

    private fun setupPreconditionsAndNavigateToEditPage(page: Page): EditUserPage =
        navigateToEditPage(page = page, admin = preconditions.farnsworth, userUnderEdit = preconditions.fry)

    private fun navigateToEditPage(page: Page, admin: PlatformUser, userUnderEdit: PlatformUser): EditUserPage {
        page.authenticateViaCookie(admin)
        page.openUsersOverviewPage()
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
