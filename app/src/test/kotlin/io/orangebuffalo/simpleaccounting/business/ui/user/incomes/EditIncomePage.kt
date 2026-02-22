package io.orangebuffalo.simpleaccounting.business.ui.user.incomes

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox.Companion.checkboxByOwnLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemCategoryInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemCurrencyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemDatePickerByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemEntitySelectByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemGeneralTaxInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemMarkdownByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemMoneyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

abstract class EditIncomePageBase(page: Page) : SaPageBase(page) {
    val generalInformationHeader = components.sectionHeader("General Information")
    val additionalInformationHeader = components.sectionHeader("Additional Information")
    val attachmentsHeader = components.sectionHeader("Attachments")

    val category = components.formItemCategoryInputByLabel("Category")
    val title = components.formItemTextInputByLabel("Description / Title")
    val currency = components.formItemCurrencyInputByLabel("Currency")
    val originalAmount = components.formItemMoneyInputByLabel("Amount")
    val dateReceived = components.formItemDatePickerByLabel("Date Received")
    val generalTax = components.formItemGeneralTaxInputByLabel("Included General Tax")
    val linkedInvoice = components.formItemEntitySelectByLabel("Linked Invoice")
    val notes = components.formItemMarkdownByLabel("Notes")
    val documentsUpload = DocumentsUpload.singleton(components.page)

    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")

    // Functions to get conditionally visible fields based on form state
    fun convertedAmountInDefaultCurrency(defaultCurrency: String) =
        components.formItemMoneyInputByLabel("Amount in $defaultCurrency")

    fun useDifferentExchangeRateForIncomeTaxPurposes() =
        components.checkboxByOwnLabel("Using different exchange rate for taxation purposes")

    fun incomeTaxableAmountInDefaultCurrency(defaultCurrency: String) =
        components.formItemMoneyInputByLabel("Amount in $defaultCurrency for taxation purposes")
}

class CreateIncomePage private constructor(page: Page) : EditIncomePageBase(page) {
    private val header = components.pageHeader("Record New Income")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateIncomePage(spec: CreateIncomePage.() -> Unit = {}) {
            CreateIncomePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateIncomePage(spec: CreateIncomePage.() -> Unit = {}) {
            navigate("/incomes/create")
            shouldBeCreateIncomePage(spec)
        }
    }
}

class EditIncomePage private constructor(page: Page) : EditIncomePageBase(page) {
    private val header = components.pageHeader("Edit Income")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditIncomePage(spec: EditIncomePage.() -> Unit = {}) {
            EditIncomePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
