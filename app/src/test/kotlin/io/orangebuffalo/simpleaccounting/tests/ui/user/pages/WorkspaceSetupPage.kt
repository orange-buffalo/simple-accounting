package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker

class WorkspaceSetupPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Workspace")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        @UiComponentMarker
        fun Page.shouldBeWorkspaceSetupPage(spec: WorkspaceSetupPage.() -> Unit) {
            WorkspaceSetupPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
