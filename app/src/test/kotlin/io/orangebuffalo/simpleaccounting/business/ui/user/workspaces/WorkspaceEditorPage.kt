package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemCurrencyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

abstract class WorkspaceEditorPageBase(
    page: Page,
    pageTitle: String
) : SaPageBase(page) {
    private val header = components.pageHeader(pageTitle)
    private val generalInfoHeader = components.sectionHeader("General Information")

    val name = components.formItemTextInputByLabel("Workspace Name")
    val defaultCurrency = components.formItemCurrencyInputByLabel("Default Currency")

    val cancelButton = components.buttonByText("Cancel")
    val saveButton = components.buttonByText("Save")

    fun shouldBeOpen(): WorkspaceEditorPageBase {
        header.shouldBeVisible()
        generalInfoHeader.shouldBeVisible()
        return this
    }
}

class CreateWorkspacePage private constructor(page: Page) : WorkspaceEditorPageBase(page, "Create New Workspace") {
    companion object {
        fun Page.shouldBeCreateWorkspacePage(spec: CreateWorkspacePage.() -> Unit = {}) {
            CreateWorkspacePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateWorkspacePage(spec: CreateWorkspacePage.() -> Unit = {}) {
            navigate("/settings/workspaces/create")
            shouldBeCreateWorkspacePage(spec)
        }
    }
}

class EditWorkspacePage private constructor(page: Page) : WorkspaceEditorPageBase(page, "Edit Workspace") {
    companion object {
        fun Page.shouldBeEditWorkspacePage(spec: EditWorkspacePage.() -> Unit = {}) {
            EditWorkspacePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openEditWorkspacePage(workspaceId: Long, spec: EditWorkspacePage.() -> Unit = {}) {
            navigate("/settings/workspaces/$workspaceId/edit")
            shouldBeEditWorkspacePage(spec)
        }
    }
}
