package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.UserOverviewItem
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.toUserOverviewItem
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class UsersOverviewFullStackTest(
    private val preconditionsFactory: PreconditionsFactory,
) {

    @Test
    fun `should provide users overview`(page: Page) {
        val preconditions = setupOverviewPreconditions()
        page.loginAs(preconditions.farnsworth)
        page.shouldHaveSideMenu().clickUsersOverview()
        page.shouldBeUsersOverviewPage()
            .pageItems {
                shouldHaveExactItems(
                    UserOverviewItem(
                        userName = "aUser",
                        userType = UserOverviewItem.regularUserType,
                        userActivation = UserOverviewItem.activeUser,
                    ),
                    UserOverviewItem(
                        userName = "B user",
                        userType = UserOverviewItem.adminUserType,
                        userActivation = UserOverviewItem.inactiveUser,
                    ),
                    UserOverviewItem(
                        userName = "C User",
                        userType = UserOverviewItem.regularUserType,
                        userActivation = UserOverviewItem.inactiveUser,
                    ),
                    UserOverviewItem(
                        userName = "Farnsworth",
                        userType = UserOverviewItem.adminUserType,
                        userActivation = UserOverviewItem.activeUser,
                    ),
                ) { it.toUserOverviewItem() }
                staticItems.forEach { it.shouldNotHaveDetails() }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
    }

    @Test
    fun `should support pagination`(page: Page) {
        val preconditions = setupPaginationPreconditions()
        page.loginAs(preconditions.farnsworth)
        page.shouldHaveSideMenu().clickUsersOverview()
        val firsPageUsers = arrayOf(
            "Farnsworth", "user 1", "user 10", "user 11", "user 12",
            "user 13", "user 14", "user 15", "user 2", "user 3"
        )
        page.shouldBeUsersOverviewPage()
            .pageItems {
                shouldHaveExactItems(*firsPageUsers) { it.title }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveExactItems(
                    "user 4", "user 5", "user 6", "user 7", "user 8", "user 9"
                ) { it.title }
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveExactItems(*firsPageUsers) { it.title }
            }
    }

    @Test
    fun `should support filtering`(page: Page) {
        val preconditions = setupFilteringPreconditions()
        page.loginAs(preconditions.farnsworth)
        page.shouldHaveSideMenu().clickUsersOverview()
        page.shouldBeUsersOverviewPage()
            .pageItems {
                // ensure all targeted items visible by default
                shouldContainItems(
                    "aBcDef", "abcdef", "ABCDEF", "qwerty"
                ) { it.title }
                // ensure we update paginator as well
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }
            .filterInput { fill("cd") }
            .pageItems {
                shouldHaveExactItems(
                    "aBcDef", "abcdef", "ABCDEF"
                ) { it.title }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
    }

    private fun setupOverviewPreconditions() = preconditionsFactory.setup {
        object {
            val farnsworth = farnsworth()

            init {
                platformUser(userName = "aUser", isAdmin = false, activated = true)
                platformUser(userName = "B user", isAdmin = true, activated = false)
                platformUser(userName = "C User", isAdmin = false, activated = false)
            }
        }
    }

    private fun setupPaginationPreconditions() = preconditionsFactory.setup {
        object {
            var farnsworth = farnsworth()

            init {
                (1..15).forEach { platformUser(userName = "user $it") }
            }
        }
    }

    private fun setupFilteringPreconditions() = preconditionsFactory.setup {
        object {
            var farnsworth = farnsworth()

            init {
                platformUser(userName = "aBcDef")
                platformUser(userName = "abcdef")
                platformUser(userName = "ABCDEF")
                platformUser(userName = "qwerty")
                // some users to enable pagination
                (1..10).forEach { platformUser(userName = "user $it") }
            }
        }
    }
}
