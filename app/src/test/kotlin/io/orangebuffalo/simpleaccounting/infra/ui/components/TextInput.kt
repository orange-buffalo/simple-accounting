package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.infra.utils.shouldBeVisible
import io.orangebuffalo.simpleaccounting.infra.utils.shouldNotBeVisible
import io.orangebuffalo.simpleaccounting.infra.utils.shouldSatisfy

class TextInput<P : Any> private constructor(
    rootLocator: Locator,
    parent: P,
) : UiComponent<P, TextInput<P>>(parent) {
    private val input = rootLocator.locator("input")
    fun fill(text: String) = input.fill(text)

    fun shouldBeVisible() = input.shouldBeVisible()

    fun shouldNotBeVisible() = input.shouldNotBeVisible()

    fun shouldHaveValue(value: String) = input.shouldSatisfy("Expecting input to have value [$value]") {
        this.inputValue() shouldBe value
    }

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
