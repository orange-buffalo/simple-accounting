package io.orangebuffalo.simpleaccounting.business.ui.user.categories

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput

abstract class EditCategoryPageBase(page: Page) : SaPageBase(page) {
    val name = components.formItemTextInputByLabel("Name")
    val description = components.formItemByLabel("Description") { TextInput.byContainer(it) }
    val income = components.checkboxByOwnLabel("Income")
    val expense = components.checkboxByOwnLabel("Expense")

    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")
}

class CreateCategoryPage private constructor(page: Page) : EditCategoryPageBase(page) {
    private val header = components.pageHeader("Create New Category")

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

class EditCategoryPage private constructor(page: Page) : EditCategoryPageBase(page) {
    private val header = components.pageHeader("Edit Category")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditCategoryPage(spec: EditCategoryPage.() -> Unit = {}) {
            EditCategoryPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
