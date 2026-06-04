package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ConfirmationDialog.Companion.confirmationDialog
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ElementTable
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemDatePickerByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

class WorkspaceAccessTokensPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader()
    private val accessLinks = WorkspaceAccessLinksTable(
        ElementTable.byContainer(page.locator(".workspace-access-tokens__table"))
    )
    private val manageSectionHeader = page.getByText("Manage Existing Temporary Access Links")
    private val validTill = components.formItemDatePickerByLabel("Valid Till")
    private val saveButton = components.buttonByText("Save")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldHaveAccessLinks(count: Int): WorkspaceAccessTokensPage {
        accessLinks.shouldHaveRows(count)
        return this
    }

    fun shouldHaveNoManageExistingLinksSection(): WorkspaceAccessTokensPage {
        manageSectionHeader.shouldBeHidden()
        return this
    }

    fun createTemporaryAccessLink(validTill: String): WorkspaceAccessTokensPage {
        this.validTill.input.fill(validTill)
        saveButton.click()
        return this
    }

    fun copyTemporaryAccessLink(rowIndex: Int): WorkspaceAccessTokensPage {
        accessLinks.copyLink(rowIndex)
        return this
    }

    fun revokeAccessLink(rowIndex: Int): WorkspaceAccessTokensPage {
        accessLinks.revoke(rowIndex)
        return this
    }

    fun confirmRevokeAccessLink(): WorkspaceAccessTokensPage {
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

private class WorkspaceAccessLinksTable(
    private val table: ElementTable,
) {

    fun shouldHaveRows(count: Int) {
        table.shouldHaveRows(count)
    }

    fun copyLink(rowIndex: Int) {
        row(rowIndex).locator(".workspace-access-tokens__copy-link").click()
    }

    fun revoke(rowIndex: Int) {
        row(rowIndex).locator("button", Locator.LocatorOptions().setHasText("Revoke")).click()
    }

    private fun row(rowIndex: Int): Locator = table.row(rowIndex)
}
