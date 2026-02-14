package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput

class CreateCategoryPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Create New Category")

    val name = components.formItemTextInputByLabel("Name")
    val description = components.formItemByLabel("Description") { TextInput.byContainer(it) }
    val income = components.checkboxByOwnLabel("Income")
    val expense = components.checkboxByOwnLabel("Expense")

    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateCategoryPage(spec: CreateCategoryPage.() -> Unit = {}) {
            CreateCategoryPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateCategoryPage(spec: CreateCategoryPage.() -> Unit = {}) {
            navigate("/settings/categories/create")
            shouldBeCreateCategoryPage(spec)
        }
    }
}
