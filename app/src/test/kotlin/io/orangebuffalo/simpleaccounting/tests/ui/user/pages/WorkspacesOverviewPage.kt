package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

@UiComponentMarker
class WorkspacePanel(
    private val components: ComponentsAccessors,
    private val container: Locator
) {
    private val nameElement = container.locator(".workspace-panel__info-panel__name h3")
    val switchButton = components.buttonByText("Switch to this workspace", container = container)
    val editButton = components.buttonByText("Edit", container = container)

    fun shouldHaveTitle(title: String): WorkspacePanel {
        nameElement.shouldHaveText(title)
        return this
    }

    fun shouldHaveSwitchButton(): WorkspacePanel {
        switchButton.shouldBeVisible()
        return this
    }

    fun shouldNotHaveSwitchButton(): WorkspacePanel {
        switchButton.shouldNotBeVisible()
        return this
    }
}

class WorkspacesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Workspaces")
    val createButton = components.buttonByText("Create new workspace")
    
    private val currentWorkspaceHeader = components.sectionHeader("Current Workspace")
    private val myOtherWorkspacesHeader = components.sectionHeader("My Other Workspaces")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldHaveCurrentWorkspace(spec: WorkspacePanel.() -> Unit = {}): WorkspacesOverviewPage {
        currentWorkspaceHeader.shouldBeVisible()
        val currentWorkspacePanel = WorkspacePanel(
            components,
            currentWorkspaceHeader.container.locator("..").locator(".workspace-panel").first()
        )
        currentWorkspacePanel.spec()
        return this
    }

    fun shouldHaveOtherWorkspaces(count: Int, spec: (WorkspacePanel) -> Unit = {}): WorkspacesOverviewPage {
        myOtherWorkspacesHeader.shouldBeVisible()
        val otherWorkspacePanels = myOtherWorkspacesHeader.container
            .locator("..").locator(".workspace-panel")
            .all()
        otherWorkspacePanels.shouldHaveSize(count)
        otherWorkspacePanels.forEach { panel ->
            WorkspacePanel(components, panel).apply(spec)
        }
        return this
    }

    fun shouldNotHaveOtherWorkspaces(): WorkspacesOverviewPage {
        myOtherWorkspacesHeader.shouldNotBeVisible()
        return this
    }

    fun getOtherWorkspaceByName(name: String): WorkspacePanel {
        val panel = myOtherWorkspacesHeader.container
            .locator("..")
            .locator(".workspace-panel")
            .filter(Locator.FilterOptions().setHasText(name))
            .first()
        return WorkspacePanel(components, panel)
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
