package io.orangebuffalo.simpleaccounting.business.ui.user.customers

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

abstract class EditCustomerPageBase(page: Page) : SaPageBase(page) {
    val generalInformationHeader = components.sectionHeader("General Information")

    val name = components.formItemTextInputByLabel("Name")

    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")
}

class CreateCustomerPage private constructor(page: Page) : EditCustomerPageBase(page) {
    private val header = components.pageHeader("Create New Customer")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateCustomerPage(spec: CreateCustomerPage.() -> Unit = {}) {
            CreateCustomerPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateCustomerPage(spec: CreateCustomerPage.() -> Unit = {}) {
            navigate("/settings/customers/create")
            shouldBeCreateCustomerPage(spec)
        }
    }
}

class EditCustomerPage private constructor(page: Page) : EditCustomerPageBase(page) {
    private val header = components.pageHeader("Edit Customer")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditCustomerPage(spec: EditCustomerPage.() -> Unit = {}) {
            EditCustomerPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
