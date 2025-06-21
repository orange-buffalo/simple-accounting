package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

class FormItem<I : UiComponent<*, *>, P : Any> private constructor(
    private val rootLocator: Locator,
    parent: P,
    inputProvider: (container: Locator) -> I
) : UiComponent<P, FormItem<I, P>>(parent) {
    private val validationErrorLocator = rootLocator.locator(".el-form-item__error")
    private val inputLocator =
        rootLocator.locator("xpath=//*[@class='el-form-item__content']/*[@class != 'el-form-item__error']")
    val input: I = inputProvider(inputLocator)

    fun shouldBeVisible () = rootLocator.shouldBeVisible()

    fun shouldBeHidden() = rootLocator.shouldBeHidden()

    fun shouldHaveValidationError(message: String): P {
        validationErrorLocator.shouldBeVisible()
        validationErrorLocator.shouldHaveText(message)
        return parent
    }

    fun shouldNotHaveValidationErrors() {
        validationErrorLocator.shouldBeHidden()
    }

    companion object {
        fun <P : SaPageBase<P>, I : UiComponent<*, *>> ComponentsAccessors<P>.formItemByLabel(
            label: String,
            inputProvider: (container: Locator) -> I
        ) = FormItem(
            rootLocator = page.locator("//*[@class='el-form-item__label' and text()='$label']/.."),
            parent = this.owner,
            inputProvider = inputProvider
        )

        fun <P : SaPageBase<P>> ComponentsAccessors<P>.formItemTextInputByLabel(label: String) =
            formItemByLabel(label) { TextInput.byContainer(it) }

        fun <P: SaPageBase<P>> ComponentsAccessors<P>.formItemSelectByLabel(label: String) =
            formItemByLabel(label) { Select.byContainer(it) }
    }
}
