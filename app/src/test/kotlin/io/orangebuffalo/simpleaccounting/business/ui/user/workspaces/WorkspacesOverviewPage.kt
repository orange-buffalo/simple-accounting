package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Paginator.Companion.twoSyncedPaginators
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

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

    private val pageableItemsContainer = page.locator(".sa-pageable-items")
    val paginator = components.twoSyncedPaginators(pageableItemsContainer)

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun getWorkspacePanelByName(name: String): WorkspacePanel {
        val panel = pageableItemsContainer.locator(".sa-pageable-items__item .workspace-panel")
            .filter(Locator.FilterOptions().setHasText(name))
            .first()
        return WorkspacePanel(panel)
    }

    fun shouldHaveWorkspaces(vararg names: String): WorkspacesOverviewPage {
        shouldSatisfy("Workspaces should match expected names") {
            pageableItemsContainer
                .locator(".sa-pageable-items__item .workspace-panel__info-panel__name h3")
                .all()
                .map { it.innerText() }
                .shouldContainExactly(*names)
        }
        return this
    }

    fun shouldHaveNoWorkspaces(): WorkspacesOverviewPage {
        pageableItemsContainer.locator(".sa-pageable-items__empty-results").shouldBeVisible()
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
