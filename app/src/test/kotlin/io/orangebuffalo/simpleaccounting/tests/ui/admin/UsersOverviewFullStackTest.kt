package io.orangebuffalo.simpleaccounting.tests.ui.admin

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.UserOverviewItem
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.openUsersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.admin.pages.toUserOverviewItem
import org.junit.jupiter.api.Test

class UsersOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should provide users overview`(page: Page) {
        val preconditions = setupOverviewPreconditions()
        page.authenticateViaCookie(preconditions.farnsworth)
        page.openUsersOverviewPage()
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
        page.authenticateViaCookie(preconditions.farnsworth)
        val firsPageUsers = arrayOf(
            "Farnsworth", "user 1", "user 10", "user 11", "user 12",
            "user 13", "user 14", "user 15", "user 2", "user 3"
        )
        page.openUsersOverviewPage()
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
        page.authenticateViaCookie(preconditions.farnsworth)
        page.openUsersOverviewPage()
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

    private fun setupOverviewPreconditions() = preconditions {
        object {
            val farnsworth = farnsworth()

            init {
                platformUser(userName = "aUser", isAdmin = false, activated = true)
                platformUser(userName = "B user", isAdmin = true, activated = false)
                platformUser(userName = "C User", isAdmin = false, activated = false)
            }
        }
    }

    private fun setupPaginationPreconditions() = preconditions {
        object {
            var farnsworth = farnsworth()

            init {
                (1..15).forEach { platformUser(userName = "user $it") }
            }
        }
    }

    private fun setupFilteringPreconditions() = preconditions {
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
