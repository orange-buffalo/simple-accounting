package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByContainer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageableItems.Companion.pageableItems
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/* language=javascript */
private const val WORKSPACE_PANEL_DATA_JS = """
    (panel) => {
        const workspacePanel = panel.querySelector('.workspace-panel');
        if (!workspacePanel) return null;
        const nameEl = workspacePanel.querySelector('.workspace-panel__info-panel__name h3');
        const switchButton = workspacePanel.querySelector('.workspace-panel__info-panel__name button');
        const switchButtonVisible = switchButton !== null && switchButton.offsetParent !== null;
        const defaultCurrencyPanel = workspacePanel.querySelector('.sa-item-attributes .sa-attribute-value');
        const defaultCurrencyValue = defaultCurrencyPanel?.querySelector(':scope > div:nth-child(2)');
        return {
            title: nameEl ? nameEl.textContent.trim() : null,
            switchButtonVisible: switchButtonVisible,
            defaultCurrency: defaultCurrencyValue ? defaultCurrencyValue.textContent.trim() : null
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
    private val switchButtonLocator = container.locator(".workspace-panel__info-panel__name button")

    fun clickSwitchButton() {
        switchButtonLocator.click()
    }
}

class WorkspacesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader()
    val createButton = components.buttonByContainer(page.locator(".sa-page-header .sa-header-options"))

    val pageItems = components.workspacePanelItems()

    private fun shouldBeOpen() {
        header.shouldBeVisible()
        createButton.shouldBeVisible()
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
