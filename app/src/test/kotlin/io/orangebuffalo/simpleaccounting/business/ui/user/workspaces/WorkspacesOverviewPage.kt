package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

@UiComponentMarker
class WorkspacePanel(
    private val item: SaOverviewItem,
) {
    fun clickSwitchAction() {
        item.clickActionMenuItem("Switch to this workspace")
    }

    fun clickSwitchActionUk() {
        item.clickActionMenuItem("Перейти до цього робочого простору")
    }

    fun shouldHaveActionMenuItems(vararg labels: String) {
        item.shouldHaveActionMenuItems(*labels)
    }

    fun clickActionMenuItem(label: String) {
        item.clickActionMenuItem(label)
    }
}

class WorkspacesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader()
    val createButton = components.buttonByText("Create new workspace")

    val pageItems = components.overviewItems()

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun getWorkspacePanelByName(name: String): WorkspacePanel {
        return WorkspacePanel(pageItems.shouldHaveItemSatisfying { it.title == name })
    }

    fun shouldHaveWorkspaces(vararg names: String): WorkspacesOverviewPage {
        pageItems.shouldHaveTitles(*names)
        return this
    }

    fun reportRenderingWithPopovers(name: String) {
        page.locator("body").reportRendering(name)
    }

    companion object {
        fun Page.shouldBeWorkspacesOverviewPage(spec: WorkspacesOverviewPage.() -> Unit = {}) {
            WorkspacesOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openWorkspacesOverviewPage(spec: WorkspacesOverviewPage.() -> Unit = {}) {
            navigate("/settings/workspaces")
            shouldBeWorkspacesOverviewPage(spec)
        }
    }
}
