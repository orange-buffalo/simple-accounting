package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat
import kotlin.reflect.KClass

class FormItem<I : Any, T : Any>(
    rootLocator: Locator,
    private val parent: T,
    inputClass: KClass<I>
) {
    private val validationErrorLocator = rootLocator.locator(".el-form-item__error")
    private val inputLocator =
        rootLocator.locator("xpath=//*[@class='el-form-item__content']/*[@class != 'el-form-item__error']")
    private val input: I = inputClass.constructors.first().call(inputLocator, Unit)

    operator fun invoke(action: I.(formItem: FormItem<I, *>) -> Unit): T {
        input.action(this)
        return parent
    }

    fun shouldHaveValidationError(message: String): T {
        validationErrorLocator.assertThat().isVisible()
        validationErrorLocator.assertThat().hasText(message)
        return parent
    }
}

fun <T : SaPageBase<T>, I : Any> ComponentsAccessors<T>.formItemByLabel(label: String, inputClass: KClass<I>) =
    FormItem(page.locator("//*[@class='el-form-item__label' and text()='$label']/.."), this.owner, inputClass)
