package io.orangebuffalo.simpleaccounting.business.ui.user.invoices

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemCurrencyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemDatePickerByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemGeneralTaxInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemMarkdownByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemMoneyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemSelectByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

abstract class EditInvoicePageBase(page: Page) : SaPageBase(page) {
    val generalInformationHeader = components.sectionHeader("General Information")
    val additionalNotesHeader = components.sectionHeader("Additional notes")
    val attachmentsHeader = components.sectionHeader("Attachments")

    val customer = components.formItemSelectByLabel("Customer")
    val title = components.formItemTextInputByLabel("Description / Title")
    val currency = components.formItemCurrencyInputByLabel("Currency")
    val amount = components.formItemMoneyInputByLabel("Amount")
    val dateIssued = components.formItemDatePickerByLabel("Date Issued")
    val dueDate = components.formItemDatePickerByLabel("Due Date")
    val generalTax = components.formItemGeneralTaxInputByLabel("Included General Tax")
    val notes = components.formItemMarkdownByLabel("Notes")
    val documentsUpload = DocumentsUpload.singleton(components.page)

    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")

    // Functions to get conditionally visible fields based on form state
    fun alreadySent() = components.checkboxByOwnLabel("Already Sent")
    fun dateSent() = components.formItemDatePickerByLabel("Date Sent")

    fun alreadyPaid() = components.checkboxByOwnLabel("Already Paid")
    fun datePaid() = components.formItemDatePickerByLabel("Date Paid")
}

class CreateInvoicePage private constructor(page: Page) : EditInvoicePageBase(page) {
    private val header = components.pageHeader("Create New Invoice")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateInvoicePage(spec: CreateInvoicePage.() -> Unit = {}) {
            CreateInvoicePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateInvoicePage(spec: CreateInvoicePage.() -> Unit = {}) {
            navigate("/invoices/create")
            shouldBeCreateInvoicePage(spec)
        }
    }
}

class EditInvoicePage private constructor(page: Page) : EditInvoicePageBase(page) {
    private val header = components.pageHeader("Edit Invoice")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditInvoicePage(spec: EditInvoicePage.() -> Unit = {}) {
            EditInvoicePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
