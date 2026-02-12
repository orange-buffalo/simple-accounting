package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

class TextInput private constructor(
    rootLocator: Locator,
) : UiComponent<TextInput>() {
    private val input = rootLocator.locator("input, textarea").first()
    fun fill(text: String) = input.fill(text)

    fun shouldBeVisible() = input.shouldBeVisible()

    fun shouldBeHidden() = input.shouldBeHidden()

    fun shouldHaveValue(value: String) = input.shouldHaveValue(value)

    fun shouldBeEnabled() = input.shouldBeEnabled()
    fun shouldBeDisabled() = input.shouldBeDisabled()

    companion object {
        fun ComponentsAccessors.textInputByPlaceholder(placeholder: String) =
            TextInput(
                page.locator(
                    "//*[${XPath.hasClass("el-input")} and .//*[@placeholder='$placeholder']]"
                )
            )

        fun byContainer(container: Locator) = TextInput(container)
    }
}
