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
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.EditUserPage.Companion.shouldBeEditUserPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.UsersOverviewPage.Companion.openUsersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.UsersOverviewPage.Companion.shouldBeUsersOverviewPage
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
        page.setupPreconditionsAndNavigateToEditPage {
            userName {
                input.shouldHaveValue(preconditions.fry.userName)
            }
            role {
                input.shouldBeDisabled()
            }
            userName {
                input.fill("fryX")
            }
            saveButton { click() }
            shouldHaveNotifications {
                success("User fryX has been successfully saved")
            }
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
        page.setupPreconditionsAndNavigateToEditPage {
            cancelButton { click() }
        }

        page.shouldBeUsersOverviewPage()
    }

    @Test
    fun `should validate input`(page: Page) {
        page.setupPreconditionsAndNavigateToEditPage {
            userName {
                // wait for page data to load
                input.shouldHaveValue(preconditions.fry.userName)
                input.fill("")
            }
            saveButton { click() }
            shouldHaveNotifications {
                validationFailed()
            }
            userName {
                shouldHaveValidationError("This value is required and should not be blank")
            }
            role {
                shouldNotHaveValidationErrors()
            }

            userName { input.fill("x".repeat(256)) }
            saveButton { click() }
            shouldHaveNotifications {
                validationFailed()
            }
            userName {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }

            userName { input.fill("x".repeat(255)) }
            saveButton { click() }
            shouldHaveNotifications {
                success()
            }
        }
    }

    @Test
    fun `should validate user name uniqueness`(page: Page) {
        page.setupPreconditionsAndNavigateToEditPage {
            userName {
                // wait for page data to load
                input.shouldHaveValue(preconditions.fry.userName)
                input.fill(preconditions.farnsworth.userName)
            }
            saveButton { click() }
            userName { shouldHaveValidationError("User with username \"${preconditions.farnsworth.userName}\" already exists") }
            shouldHaveNotifications {
                validationFailed()
            }
        }
    }

    @Test
    fun `should not fail with validation if user name is not changed`(page: Page) {
        page.setupPreconditionsAndNavigateToEditPage {
            userName {
                // wait for page data to load
                input.shouldHaveValue(preconditions.fry.userName)
            }
            saveButton { click() }
            shouldHaveNotifications {
                success()
            }
        }
    }

    @Test
    fun `should render activated status`(page: Page) {
        page.setupPreconditionsAndNavigateToEditPage {
            activationStatus {
                shouldBeVisible()
                input { shouldBeActivated() }
            }
        }
    }

    @Test
    fun `should retrieve valid activation token`(page: Page) {
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
        page.navigateToEditPage(data.farnsworth, data.user) {
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
        page.navigateToEditPage(data.farnsworth, data.user) {
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
        val data = preconditions {
            object {
                val farnsworth = farnsworth()
                val user = platformUser(
                    userName = "user",
                    activated = false,
                )
            }
        }
        page.navigateToEditPage(data.farnsworth, data.user) {
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
    }

    private fun Page.setupPreconditionsAndNavigateToEditPage(spec: EditUserPage.() -> Unit) =
        navigateToEditPage(admin = preconditions.farnsworth, userUnderEdit = preconditions.fry, spec)

    private fun Page.navigateToEditPage(
        admin: PlatformUser,
        userUnderEdit: PlatformUser,
        spec: EditUserPage.() -> Unit
    ) {
        authenticateViaCookie(admin)
        openUsersOverviewPage {
            pageItems {
                finishLoadingWhenTimeMocked()
                val userUnderEditItem = shouldHaveItemSatisfying {
                    it.title == userUnderEdit.userName
                }
                userUnderEditItem.executeAction("Edit")
            }
        }
        shouldBeEditUserPage(spec)
    }
}
