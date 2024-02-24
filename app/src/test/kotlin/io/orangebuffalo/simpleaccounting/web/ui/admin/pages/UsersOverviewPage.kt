package io.orangebuffalo.simpleaccounting.web.ui.admin.pages

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaOverviewItem
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaPageBase

class UsersOverviewPage(page: Page) : SaPageBase<UsersOverviewPage>(page) {
    val pageItems = components.overviewItems()
    private val header = components.pageHeader("Users")
    fun shouldBeOpen() = header.shouldBeVisible()
}

data class UserOverviewItem(
    val userName: String,
    val userType: SaOverviewItem.PrimaryAttribute,
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
    }
}

fun SaOverviewItem.toUserOverviewItem(): UserOverviewItem {
    return UserOverviewItem(
        userName = title.shouldNotBeNull(),
        userType = primaryAttributes.shouldHaveSize(1).first(),
    )
}

fun Page.shouldBeUsersOverviewPage(): UsersOverviewPage = UsersOverviewPage(this).shouldBeOpen()
