package io.orangebuffalo.simpleaccounting.tests.ui.user.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemCategoryInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemCheckboxByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemCurrencyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemDatePickerByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemGeneralTaxInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemInputNumberByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemMoneyInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemNotesInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu

abstract class EditExpensePageBase(page: Page) : SaPageBase(page) {
    val generalInformationHeader = components.sectionHeader("General Information")
    val additionalInformationHeader = components.sectionHeader("Additional Information")
    val attachmentsHeader = components.sectionHeader("Attachments")

    val category = components.formItemCategoryInputByLabel("Category")
    val title = components.formItemTextInputByLabel("Description / Title")
    val currency = components.formItemCurrencyInputByLabel("Currency")
    val originalAmount = components.formItemMoneyInputByLabel("Original Amount")
    val datePaid = components.formItemDatePickerByLabel("Date Paid")
    val generalTax = components.formItemGeneralTaxInputByLabel("Included General Tax")
    val notes = components.formItemNotesInputByLabel("Notes")
    val documentsUpload = DocumentsUpload.byContainer(components.page.locator("body"))

    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")

    // Functions to get conditionally visible fields based on form state
    fun convertedAmountInDefaultCurrency(defaultCurrency: String) =
        components.formItemMoneyInputByLabel("Amount in $defaultCurrency")

    fun useDifferentExchangeRateForIncomeTaxPurposes() =
        components.formItemCheckboxByLabel("Using different exchange rate for taxation purposes")

    fun incomeTaxableAmountInDefaultCurrency(defaultCurrency: String) =
        components.formItemMoneyInputByLabel("Amount in $defaultCurrency for taxation purposes")

    fun partialForBusiness() =
        components.formItemCheckboxByLabel("Partial Business Purpose")

    fun percentOnBusiness() =
        components.formItemInputNumberByLabel("% related to business activities")
}

class CreateExpensePage private constructor(page: Page) : EditExpensePageBase(page) {
    private val header = components.pageHeader("Record New Expense")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateExpensePage(spec: CreateExpensePage.() -> Unit = {}) {
            CreateExpensePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateExpensePage(spec: CreateExpensePage.() -> Unit = {}) {
            navigate("/expenses/create")
            shouldBeCreateExpensePage(spec)
        }
    }
}

class EditExpensePage private constructor(page: Page) : EditExpensePageBase(page) {
    private val header = components.pageHeader("Edit Expense")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditExpensePage(spec: EditExpensePage.() -> Unit = {}) {
            EditExpensePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
