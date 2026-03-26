package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageableItems.Companion.pageableItems
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/* language=javascript */
private const val WORKSPACE_PANEL_DATA_JS = """
    (panel) => {
        const workspacePanel = panel.querySelector('.workspace-panel');
        if (!workspacePanel) return null;
        const nameEl = workspacePanel.querySelector('.workspace-panel__info-panel__name h3');
        const switchButton = workspacePanel.querySelector('.workspace-panel__info-panel__name button');
        const switchButtonVisible = switchButton !== null
            && switchButton.textContent.trim() === 'Switch to this workspace'
            && switchButton.offsetParent !== null;
        let defaultCurrency = null;
        const labels = workspacePanel.querySelectorAll('.sa-attribute-value__label');
        for (const label of labels) {
            if (label.textContent.trim() === 'Default Currency') {
                const valueEl = label.nextElementSibling;
                if (valueEl) {
                    defaultCurrency = valueEl.textContent.trim();
                }
            }
        }
        return {
            title: nameEl ? nameEl.textContent.trim() : null,
            switchButtonVisible: switchButtonVisible,
            defaultCurrency: defaultCurrency
        };
    };
"""

@Serializable
data class WorkspacePanelData(
    val title: String? = null,
    val switchButtonVisible: Boolean = false,
    val defaultCurrency: String? = null,
)

@UiComponentMarker
class WorkspacePanel(
    private val container: Locator
) {
    private val switchButtonLocator = container.locator("xpath=//button[${XPath.hasText("Switch to this workspace")}]")

    fun clickSwitchButton() {
        switchButtonLocator.click()
    }
}

class WorkspacesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Workspaces")
    val createButton = components.buttonByText("Create new workspace")

    val pageItems = components.workspacePanelItems()

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun getWorkspacePanelByName(name: String): WorkspacePanel {
        val item = pageItems.shouldHaveItemSatisfying { wrapper ->
            wrapper.container.locator(".workspace-panel__info-panel__name h3")
                .innerText().trim() == name
        }
        return WorkspacePanel(item.container.locator(".workspace-panel"))
    }

    fun shouldHaveWorkspaces(vararg names: String): WorkspacesOverviewPage {
        pageItems.shouldHaveDataSatisfying { items ->
            items.map { it.title }.shouldContainExactly(*names)
        }
        return this
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

class WorkspacePanelItemWrapper(val container: Locator)

private fun ComponentsAccessors.workspacePanelItems() =
    pageableItems(
        itemDataJs = WORKSPACE_PANEL_DATA_JS,
        itemDataSerializer = serializer<WorkspacePanelData>(),
    ) { container -> WorkspacePanelItemWrapper(container) }

