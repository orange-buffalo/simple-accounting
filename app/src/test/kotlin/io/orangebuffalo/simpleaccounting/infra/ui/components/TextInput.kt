package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.XPath

class TextInput<T : Any>(
    rootLocator: Locator,
    private val parent: T,
) {
    private val input = rootLocator.locator("input")
    fun fill(text: String) = input.fill(text)

    operator fun invoke(action: TextInput<*>.() -> Unit): T {
        this.action()
        return parent
    }
}

fun <T : SaPageBase<T>> ComponentsAccessors<T>.textInputByPlaceholder(placeholder: String) =
    TextInput(
        page.locator(
            "//*[${XPath.hasClass("el-input")} and .//*[@placeholder='$placeholder']]"
        ), this.owner
    )
