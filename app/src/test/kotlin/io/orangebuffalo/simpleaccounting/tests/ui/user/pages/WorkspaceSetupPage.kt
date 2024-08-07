package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class WorkspaceSetupPage(page: Page) : SaPageBase<WorkspaceSetupPage>(page) {
    private val header = components.pageHeader("Workspace")

    fun shouldBeOpen() = header.shouldBeVisible()
}

fun Page.shouldBeWorkspaceSetupPage(): WorkspaceSetupPage = WorkspaceSetupPage(this).shouldBeOpen()
