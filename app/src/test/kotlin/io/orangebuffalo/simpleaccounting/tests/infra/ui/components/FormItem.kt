package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

class FormItem<I : UiComponent<*>> private constructor(
    private val rootLocator: Locator,
    inputProvider: (container: Locator) -> I
) : UiComponent<FormItem<I>>() {
    private val validationErrorLocator = rootLocator.locator(".el-form-item__error")
    private val inputLocator =
        rootLocator.locator(".el-form-item__content > :not(.el-form-item__error)")
    val input: I = inputProvider(inputLocator)

    fun shouldBeVisible () = rootLocator.shouldBeVisible()

    fun shouldBeHidden() = rootLocator.shouldBeHidden()

    fun shouldHaveValidationError(message: String) {
        validationErrorLocator.shouldBeVisible()
        validationErrorLocator.shouldHaveText(message)
    }

    fun shouldNotHaveValidationErrors() {
        validationErrorLocator.shouldBeHidden()
    }

    fun shouldBeLoading() {
        rootLocator.locator(".sa-input-loader__indicator").shouldBeVisible()
    }

    companion object {
        fun <I : UiComponent<*>> ComponentsAccessors.formItemByLabel(
            label: String,
            inputProvider: (container: Locator) -> I
        ) = FormItem(
            rootLocator = page.locator("//*[@class='el-form-item__label' and text()='$label']/.."),
            inputProvider = inputProvider
        )

        fun ComponentsAccessors.formItemTextInputByLabel(label: String) =
            formItemByLabel(label) { TextInput.byContainer(it) }

        fun ComponentsAccessors.formItemSelectByLabel(label: String) =
            formItemByLabel(label) { Select.byContainer(it) }

        fun ComponentsAccessors.formItemMoneyInputByLabel(label: String) =
            formItemByLabel(label) { MoneyInput.byContainer(it) }

        fun ComponentsAccessors.formItemCategoryInputByLabel(label: String) =
            formItemByLabel(label) { Select.byContainer(it) }

        fun ComponentsAccessors.formItemCurrencyInputByLabel(label: String) =
            formItemByLabel(label) { CurrencyInput.byContainer(it) }

        fun ComponentsAccessors.formItemDatePickerByLabel(label: String) =
            formItemByLabel(label) { DatePicker.byContainer(it) }

        fun ComponentsAccessors.formItemInputNumberByLabel(label: String) =
            formItemByLabel(label) { InputNumber.byContainer(it) }

        fun ComponentsAccessors.formItemGeneralTaxInputByLabel(label: String) =
            formItemByLabel(label) { Select.byContainer(it) }

        fun ComponentsAccessors.formItemMarkdownByLabel(label: String) =
            formItemByLabel(label) { Markdown.byContainer(it) }

        fun ComponentsAccessors.formItemDocumentsUploadByLabel(label: String) =
            formItemByLabel(label) { DocumentsUpload.byContainer(it) }

        fun ComponentsAccessors.formItemCheckboxByLabel(label: String) =
            formItemByLabel(label) { Checkbox.checkboxByOwnLabel(it, label) }
    }
}
