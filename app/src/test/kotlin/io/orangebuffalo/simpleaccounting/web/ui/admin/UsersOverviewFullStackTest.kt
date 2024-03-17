package io.orangebuffalo.simpleaccounting.web.ui.admin

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.UserOverviewItem
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.shouldBeUsersOverviewPage
import io.orangebuffalo.simpleaccounting.web.ui.admin.pages.toUserOverviewItem
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test

@SimpleAccountingFullStackTest
class UsersOverviewFullStackTest {

    @Test
    fun `should provide users overview`(page: Page, testData: OverviewTestData) {
        page.loginAs(testData.farnsworth)
        page.shouldHaveSideMenu().clickUsersOverview()
        page.shouldBeUsersOverviewPage()
            .pageItems {
                shouldSatisfy { items ->
                    items.map { it.toUserOverviewItem() }.shouldContainExactly(
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
                    )
                    items.forEach { it.shouldNotHaveDetails() }
                }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
    }

    @Test
    fun `should support pagination`(page: Page, testData: PaginationTestData) {
        page.loginAs(testData.farnsworth)
        page.shouldHaveSideMenu().clickUsersOverview()
        val firsPageUsers = listOf(
            "Farnsworth", "user 1", "user 10", "user 11", "user 12",
            "user 13", "user 14", "user 15", "user 2", "user 3"
        )
        page.shouldBeUsersOverviewPage()
            .pageItems {
                shouldSatisfy { items ->
                    items.map { it.title }.shouldContainExactly(firsPageUsers)
                }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldSatisfy { items ->
                    items.map { it.title }.shouldContainExactly(
                        "user 4", "user 5", "user 6", "user 7", "user 8", "user 9"
                    )
                }
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldSatisfy { items ->
                    items.map { it.title }.shouldContainExactly(firsPageUsers)
                }
            }
    }

    @Test
    fun `should support filtering`(page: Page, testData: FilteringTestData) {
        page.loginAs(testData.farnsworth)
        page.shouldHaveSideMenu().clickUsersOverview()
        page.shouldBeUsersOverviewPage()
            .pageItems {
                // ensure all targeted items visible by default
                shouldSatisfy { items ->
                    items.map { it.title }.shouldContainAll(
                        "aBcDef", "abcdef", "ABCDEF", "qwerty"
                    )
                }
                // ensure we update paginator as well
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }
            .filterInput { fill("cd") }
            .pageItems {
                shouldSatisfy { items ->
                    items.map { it.title }.shouldContainExactly("aBcDef", "abcdef", "ABCDEF")
                }
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
    }

    class OverviewTestData : TestDataDeprecated {

        var farnsworth = Prototypes.farnsworth()
        override fun generateData() = listOf(
            farnsworth,
            Prototypes.platformUser(userName = "aUser", isAdmin = false, activated = true),
            Prototypes.platformUser(userName = "B user", isAdmin = true, activated = false),
            Prototypes.platformUser(userName = "C User", isAdmin = false, activated = false),
        )
    }

    class PaginationTestData : TestDataDeprecated {
        var farnsworth = Prototypes.farnsworth()
        override fun generateData() = listOf(
            farnsworth,
            *((1..15)
                .map { Prototypes.platformUser(userName = "user $it") }
                .toTypedArray())
        )
    }

    class FilteringTestData : TestDataDeprecated {
        var farnsworth = Prototypes.farnsworth()
        override fun generateData() = listOf(
            farnsworth,
            Prototypes.platformUser(userName = "aBcDef"),
            Prototypes.platformUser(userName = "abcdef"),
            Prototypes.platformUser(userName = "ABCDEF"),
            Prototypes.platformUser(userName = "qwerty"),
            // some users to enable pagination
            *((1..10)
                .map { Prototypes.platformUser(userName = "user $it") }
                .toTypedArray())
        )
    }
}
