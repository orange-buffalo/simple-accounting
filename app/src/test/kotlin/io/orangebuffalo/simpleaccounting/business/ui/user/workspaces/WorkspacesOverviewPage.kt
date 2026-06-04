package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ConfirmationDialog.Companion.confirmationDialog
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemDatePickerByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageableItems.Companion.pageableItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
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
    private val page: Page,
    private val container: Locator,
) {
    private val switchButtonLocator = container.locator(".workspace-panel__info-panel__name button")
    private val actionsButton = container.locator(".workspace-panel__actions-trigger")

    fun clickSwitchButton() {
        switchButtonLocator.click()
    }

    fun openActionsMenu() {
        actionsButton.click()
    }

    fun shouldHaveActionMenuItems(vararg labels: String) {
        openActionsMenu()
        page.locator(".workspace-panel__actions-menu .el-button").allInnerTexts()
            .map { it.trim() }
            .shouldContainExactlyInAnyOrder(*labels)
    }

    fun clickActionMenuItem(label: String) {
        openActionsMenu()
        page.locator(".workspace-panel__actions-menu .el-button", Page.LocatorOptions().setHasText(label)).click()
    }
}

class WorkspacesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader()
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
        return WorkspacePanel(page, item.container.locator(".workspace-panel"))
    }

    fun shouldHaveWorkspaces(vararg names: String): WorkspacesOverviewPage {
        pageItems.shouldHaveDataSatisfying { items ->
            items.map { it.title }.shouldContainExactly(*names)
        }
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

class WorkspaceAccessTokensPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader()
    private val tableRows = page.locator(".workspace-access-tokens__table .el-table__body tbody tr")
    private val manageSectionHeader = page.getByText("Manage Existing Temporary Access Links")
    private val validTill = components.formItemDatePickerByLabel("Valid Till")
    private val saveButton = components.buttonByText("Save")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldHaveAccessTokens(count: Int): WorkspaceAccessTokensPage {
        tableRows.shouldHaveCount(count)
        return this
    }

    fun shouldHaveNoManageExistingTokensSection(): WorkspaceAccessTokensPage {
        manageSectionHeader.shouldBeHidden()
        return this
    }

    fun createShareLink(validTill: String): WorkspaceAccessTokensPage {
        this.validTill.input.fill(validTill)
        saveButton.click()
        return this
    }

    fun copyTemporaryAccessLink(rowIndex: Int): WorkspaceAccessTokensPage {
        tableRows.nth(rowIndex).locator(".workspace-access-tokens__copy-link").click()
        return this
    }

    fun revokeAccessToken(rowIndex: Int): WorkspaceAccessTokensPage {
        tableRows.nth(rowIndex).locator("button", Locator.LocatorOptions().setHasText("Revoke")).click()
        return this
    }

    fun confirmRevokeAccessToken(): WorkspaceAccessTokensPage {
        components.confirmationDialog()
            .shouldBeVisible()
            .clickButton("Revoke")
        return this
    }

    fun reportRenderingWithPopovers(name: String) {
        page.locator("body").reportRendering(name)
    }

    companion object {
        fun Page.shouldBeWorkspaceAccessTokensPage(spec: WorkspaceAccessTokensPage.() -> Unit = {}) {
            WorkspaceAccessTokensPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openWorkspaceAccessTokensPage(workspaceId: String, spec: WorkspaceAccessTokensPage.() -> Unit = {}) {
            navigate("/settings/workspaces/$workspaceId/access-tokens")
            shouldBeWorkspaceAccessTokensPage(spec)
        }
    }
}

class WorkspacePanelItemWrapper(val container: Locator)

private fun ComponentsAccessors.workspacePanelItems() =
    pageableItems(
        itemDataJs = WORKSPACE_PANEL_DATA_JS,
        itemDataSerializer = serializer<WorkspacePanelData>(),
    ) { container -> WorkspacePanelItemWrapper(container) }
