package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class FormItem<I : Any, T : Any> private constructor(
    rootLocator: Locator,
    private val parent: T,
    inputProvider: (container: Locator) -> I
) {
    private val validationErrorLocator = rootLocator.locator(".el-form-item__error")
    private val inputLocator =
        rootLocator.locator("xpath=//*[@class='el-form-item__content']/*[@class != 'el-form-item__error']")
    private val input: I = inputProvider(inputLocator)

    operator fun invoke(action: I.(formItem: FormItem<I, *>) -> Unit): T {
        input.action(this)
        return parent
    }

    fun shouldHaveValidationError(message: String): T {
        validationErrorLocator.assertThat().isVisible()
        validationErrorLocator.assertThat().hasText(message)
        return parent
    }

    companion object {
        private fun <T : SaPageBase<T>, I : Any> ComponentsAccessors<T>.formItemByLabel(
            label: String,
            inputProvider: (container: Locator) -> I
        ) = FormItem(
            rootLocator = page.locator("//*[@class='el-form-item__label' and text()='$label']/.."),
            parent = this.owner,
            inputProvider = inputProvider
        )

        fun <T : SaPageBase<T>> ComponentsAccessors<T>.formItemTextInputByLabel(label: String) =
            formItemByLabel(label) { TextInput.byContainer(it) }
    }
}
