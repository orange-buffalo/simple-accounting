package io.orangebuffalo.simpleaccounting.business.ui.admin.usermanagement

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewFilters.Companion.overviewFilters
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class UsersOverviewPage private constructor(page: Page) : SaPageBase(page) {
    val pageItems = components.overviewItems()
    val filters = components.overviewFilters()
    private val header = components.pageHeader("Users")
    val createUserButton = components.buttonByText("Create user")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
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
