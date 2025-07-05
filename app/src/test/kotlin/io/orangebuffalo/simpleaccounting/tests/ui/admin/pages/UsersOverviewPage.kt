package io.orangebuffalo.simpleaccounting.tests.ui.admin.pages

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput.Companion.textInputByPlaceholder

class UsersOverviewPage(page: Page) : SaPageBase<UsersOverviewPage>(page) {
    val pageItems = components.overviewItems()
    val filterInput = components.textInputByPlaceholder("Search users")
    private val header = components.pageHeader("Users")
    val createUserButton = components.buttonByText("Create user")

    fun shouldBeOpen() = header.shouldBeVisible()
}

data class UserOverviewItem(
    val userName: String,
    val userType: SaOverviewItem.PrimaryAttribute,
    val userActivation: SaOverviewItem.PrimaryAttribute,
) {
    companion object {
        val adminUserType = SaOverviewItem.PrimaryAttribute(
            icon = "admin-user",
            text = "Admin user",
        )
        val regularUserType = SaOverviewItem.PrimaryAttribute(
            icon = "regular-user",
            text = "User",
        )
        val activeUser = SaOverviewItem.PrimaryAttribute(
            icon = "active-user",
            text = "Active",
        )
        val inactiveUser = SaOverviewItem.PrimaryAttribute(
            icon = "inactive-user",
            text = "Not yet activated",
        )
    }
}

fun SaOverviewItem.toUserOverviewItem(): UserOverviewItem {
    primaryAttributes.shouldHaveSize(2)
    return UserOverviewItem(
        userName = title.shouldNotBeNull(),
        userType = primaryAttributes[0],
        userActivation = primaryAttributes[1],
    )
}

fun Page.shouldBeUsersOverviewPage(): UsersOverviewPage = UsersOverviewPage(this).shouldBeOpen()

fun Page.openUsersOverviewPage(): UsersOverviewPage {
    this.navigate("/admin/users")
    return shouldBeUsersOverviewPage()
}
