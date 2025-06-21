package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

class TextInput<P : Any> private constructor(
    rootLocator: Locator,
    parent: P,
) : UiComponent<P, TextInput<P>>(parent) {
    private val input = rootLocator.locator("input")
    fun fill(text: String) = input.fill(text)

    fun shouldBeVisible() = input.shouldBeVisible()

    fun shouldBeHidden() = input.shouldBeHidden()

    fun shouldHaveValue(value: String) = input.shouldHaveValue(value)

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.textInputByPlaceholder(placeholder: String) =
            TextInput(
                page.locator(
                    "//*[${XPath.hasClass("el-input")} and .//*[@placeholder='$placeholder']]"
                ), this.owner
            )

        fun byContainer(container: Locator) = TextInput(container, Unit)
    }
}
