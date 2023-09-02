package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator

class TextInput<T : Any>(
    private val locator: Locator,
    private val parent: T,
) {
    fun fill(text: String) = locator.fill(text)

    operator fun invoke(action: TextInput<*>.() -> Unit): T {
        this.action()
        return parent
    }
}

fun <T : SaPageBase<T>> ComponentsAccessors<T>.textInputByPlaceholder(placeholder: String) =
    TextInput(page.getByPlaceholder(placeholder), this.owner)
