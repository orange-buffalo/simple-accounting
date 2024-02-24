package io.orangebuffalo.simpleaccounting.web.ui.admin

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
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
            .pageItems { items ->
                items.map { it.toUserOverviewItem() }.shouldContainExactly(
                    UserOverviewItem(
                        userName = "aUser",
                        userType = UserOverviewItem.regularUserType,
                    ),
                    UserOverviewItem(
                        userName = "B user",
                        userType = UserOverviewItem.adminUserType,
                    ),
                    UserOverviewItem(
                        userName = "C User",
                        userType = UserOverviewItem.regularUserType,
                    ),
                    UserOverviewItem(
                        userName = "Farnsworth",
                        userType = UserOverviewItem.adminUserType,
                    ),
                )
                items.forEach { it.shouldNotHaveDetails() }
            }
    }

    class OverviewTestData : TestData {

        var farnsworth = Prototypes.farnsworth()
        override fun generateData() = listOf(
            farnsworth,
            Prototypes.platformUser(userName = "aUser", isAdmin = false),
            Prototypes.platformUser(userName = "B user", isAdmin = true),
            Prototypes.platformUser(userName = "C User", isAdmin = false),
        )
    }
}
