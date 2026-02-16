package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

@UiComponentMarker
class WorkspacePanel(
    private val container: Locator
) {
    private val nameElement = container.locator(".workspace-panel__info-panel__name h3")
    private val switchButtonLocator = container.locator("xpath=//button[${XPath.hasText("Switch to this workspace")}]")

    fun shouldHaveTitle(title: String): WorkspacePanel {
        nameElement.shouldHaveText(title)
        return this
    }

    fun shouldHaveSwitchButton(): WorkspacePanel {
        switchButtonLocator.shouldBeVisible()
        return this
    }

    fun shouldNotHaveSwitchButton(): WorkspacePanel {
        switchButtonLocator.shouldBeHidden()
        return this
    }
    
    fun clickSwitchButton() {
        switchButtonLocator.click()
    }
}

class WorkspacesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Workspaces")
    val createButton = components.buttonByText("Create new workspace")
    
    private val currentWorkspaceHeaderLocator = page.locator("//h2[${XPath.hasText("Current Workspace")}]")
    private val myOtherWorkspacesHeaderLocator = page.locator("//h2[${XPath.hasText("My Other Workspaces")}]")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldHaveCurrentWorkspace(spec: WorkspacePanel.() -> Unit = {}): WorkspacesOverviewPage {
        currentWorkspaceHeaderLocator.shouldBeVisible()
        // The panel is a sibling following the h2
        val currentWorkspacePanel = WorkspacePanel(
            currentWorkspaceHeaderLocator.locator("+ .workspace-panel")
        )
        currentWorkspacePanel.spec()
        return this
    }

    fun shouldHaveOtherWorkspaces(count: Int, spec: (WorkspacePanel) -> Unit = {}): WorkspacesOverviewPage {
        myOtherWorkspacesHeaderLocator.shouldBeVisible()
        // Get all workspace panels that come after "My Other Workspaces" header
        val otherWorkspacePanels = page.locator("//h2[${XPath.hasText("My Other Workspaces")}]/following-sibling::div[@class='workspace-panel']")
            .all()
        otherWorkspacePanels.shouldHaveSize(count)
        otherWorkspacePanels.forEach { panel: Locator ->
            WorkspacePanel(panel).apply(spec)
        }
        return this
    }

    fun shouldNotHaveOtherWorkspaces(): WorkspacesOverviewPage {
        myOtherWorkspacesHeaderLocator.shouldBeHidden()
        return this
    }

    fun getOtherWorkspaceByName(name: String): WorkspacePanel {
        val panel = page.locator("//h2[${XPath.hasText("My Other Workspaces")}]/following-sibling::div[@class='workspace-panel']")
            .filter(Locator.FilterOptions().setHasText(name))
            .first()
        return WorkspacePanel(panel)
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
