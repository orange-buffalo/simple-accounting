package io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

abstract class EditGeneralTaxPageBase(page: Page, ukrainian: Boolean = false) : SaPageBase(page) {
    val title = components.formItemTextInputByLabel(if (ukrainian) "Назва" else "Title")
    val description = components.formItemTextInputByLabel(if (ukrainian) "Опис" else "Description")
    val rate = components.formItemTextInputByLabel(if (ukrainian) "Ставка (%)" else "Rate (%)")

    val saveButton = components.buttonByText(if (ukrainian) "Зберегти" else "Save")
    val cancelButton = components.buttonByText(if (ukrainian) "Скасувати" else "Cancel")
}

class CreateGeneralTaxPage private constructor(page: Page, ukrainian: Boolean) :
    EditGeneralTaxPageBase(page = page, ukrainian = ukrainian) {
    private val header = components.pageHeader(
        if (ukrainian) "Створити новий загальний податок" else "Create New General Tax"
    )

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateGeneralTaxPage(
            ukrainian: Boolean = false,
            spec: CreateGeneralTaxPage.() -> Unit = {},
        ) {
            CreateGeneralTaxPage(page = this, ukrainian = ukrainian).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateGeneralTaxPage(ukrainian: Boolean = false, spec: CreateGeneralTaxPage.() -> Unit = {}) {
            navigate("/settings/general-taxes/create")
            shouldBeCreateGeneralTaxPage(ukrainian = ukrainian, spec = spec)
        }
    }
}

class EditGeneralTaxPage private constructor(page: Page) : EditGeneralTaxPageBase(page) {
    private val header = components.pageHeader("Edit General Tax")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditGeneralTaxPage(spec: EditGeneralTaxPage.() -> Unit = {}) {
            EditGeneralTaxPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
