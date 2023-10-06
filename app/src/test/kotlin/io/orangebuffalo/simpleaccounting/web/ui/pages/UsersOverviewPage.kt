package io.orangebuffalo.simpleaccounting.web.ui.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.infra.ui.components.pageHeader

class UsersOverviewPage(page: Page) : SaPageBase<UsersOverviewPage>(page) {
    private val header = components.pageHeader("Users")
    fun shouldBeOpen() = header.shouldBeVisible()
}

fun Page.shouldBeUsersOverviewPage(): UsersOverviewPage = UsersOverviewPage(this).shouldBeOpen()
