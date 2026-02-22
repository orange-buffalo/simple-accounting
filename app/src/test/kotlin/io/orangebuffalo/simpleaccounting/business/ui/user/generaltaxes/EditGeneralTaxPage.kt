package io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

abstract class EditGeneralTaxPageBase(page: Page) : SaPageBase(page) {
    val generalInformationHeader = components.sectionHeader("General Information")

    val title = components.formItemTextInputByLabel("Title")
    val description = components.formItemTextInputByLabel("Description")
    val rate = components.formItemTextInputByLabel("Rate")

    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")
}

class CreateGeneralTaxPage private constructor(page: Page) : EditGeneralTaxPageBase(page) {
    private val header = components.pageHeader("Create New General Tax")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateGeneralTaxPage(spec: CreateGeneralTaxPage.() -> Unit = {}) {
            CreateGeneralTaxPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateGeneralTaxPage(spec: CreateGeneralTaxPage.() -> Unit = {}) {
            navigate("/settings/general-taxes/create")
            shouldBeCreateGeneralTaxPage(spec)
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
