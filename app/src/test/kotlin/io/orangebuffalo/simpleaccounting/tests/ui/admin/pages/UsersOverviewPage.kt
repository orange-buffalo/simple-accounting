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
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

class UsersOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val container = page.locator(".users-overview-page")
    val pageItems = components.overviewItems()
    val filterInput = components.textInputByPlaceholder("Search users")
    private val header = components.pageHeader("Users")
    val createUserButton = components.buttonByText("Create user")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun reportRendering(name: String) {
        container.reportRendering(name)
    }

    companion object {
        fun Page.shouldBeUsersOverviewPage(spec: UsersOverviewPage.() -> Unit = {}) {
            UsersOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openUsersOverviewPage(spec: UsersOverviewPage.() -> Unit) {
            navigate("/admin/users")
            shouldBeUsersOverviewPage(spec)
        }
    }
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
