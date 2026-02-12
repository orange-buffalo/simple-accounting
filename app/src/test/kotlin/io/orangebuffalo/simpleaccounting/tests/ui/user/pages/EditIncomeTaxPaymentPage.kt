package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemDatePickerByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemMarkdownByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemMoneyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

abstract class EditIncomeTaxPaymentPageBase(page: Page) : SaPageBase(page) {
    val generalInformationHeader = components.sectionHeader("General Information")
    val additionalInformationHeader = components.sectionHeader("Additional notes")
    val attachmentsHeader = components.sectionHeader("Attachments")

    val title = components.formItemTextInputByLabel("Description / Title")
    val amount = components.formItemMoneyInputByLabel("Amount")
    val datePaid = components.formItemDatePickerByLabel("Date Paid")
    val reportingDate = components.formItemDatePickerByLabel("Reporting Date")
    val notes = components.formItemMarkdownByLabel("Notes")
    val documentsUpload = DocumentsUpload.singleton(components.page)

    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")
}

class CreateIncomeTaxPaymentPage private constructor(page: Page) : EditIncomeTaxPaymentPageBase(page) {
    private val header = components.pageHeader("Record New Income Tax Payment")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateIncomeTaxPaymentPage(spec: CreateIncomeTaxPaymentPage.() -> Unit = {}) {
            CreateIncomeTaxPaymentPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateIncomeTaxPaymentPage(spec: CreateIncomeTaxPaymentPage.() -> Unit = {}) {
            navigate("/income-tax-payments/create")
            shouldBeCreateIncomeTaxPaymentPage(spec)
        }
    }
}

class EditIncomeTaxPaymentPage private constructor(page: Page) : EditIncomeTaxPaymentPageBase(page) {
    private val header = components.pageHeader("Edit Income Tax Payment")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditIncomeTaxPaymentPage(spec: EditIncomeTaxPaymentPage.() -> Unit = {}) {
            EditIncomeTaxPaymentPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
