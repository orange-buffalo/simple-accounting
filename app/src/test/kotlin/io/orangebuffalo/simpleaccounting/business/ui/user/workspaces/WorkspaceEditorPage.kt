package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemCurrencyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

class WorkspaceEditorPage private constructor(
    page: Page,
    private val pageTitle: String
) : SaPageBase(page) {
    private val header = components.pageHeader(pageTitle)
    private val generalInfoHeader = components.sectionHeader("General Information")

    val name = components.formItemTextInputByLabel("Workspace Name")
    val defaultCurrency = components.formItemCurrencyInputByLabel("Default Currency")

    val cancelButton = components.buttonByText("Cancel")
    val saveButton = components.buttonByText("Save")

    private fun shouldBeOpen(): WorkspaceEditorPage {
        header.shouldBeVisible()
        generalInfoHeader.shouldBeVisible()
        return this
    }

    companion object {
        fun Page.shouldBeCreateWorkspacePage(spec: WorkspaceEditorPage.() -> Unit = {}) {
            WorkspaceEditorPage(this, "Create New Workspace").apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateWorkspacePage(spec: WorkspaceEditorPage.() -> Unit = {}) {
            navigate("/settings/workspaces/create")
            shouldBeCreateWorkspacePage(spec)
        }

        fun Page.shouldBeEditWorkspacePage(spec: WorkspaceEditorPage.() -> Unit = {}) {
            WorkspaceEditorPage(this, "Edit Workspace").apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
