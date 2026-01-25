package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue

class Markdown private constructor(
    private val rootLocator: Locator,
) : UiComponent<Markdown>() {
    private val textareaLocator = rootLocator.locator(".sa-notes-input__input textarea")
    private val previewLocator = rootLocator.locator(".sa-notes-input__preview")

    fun fill(text: String) = textareaLocator.fill(text)

    fun shouldBeVisible() = textareaLocator.shouldBeVisible()

    fun shouldHaveValue(value: String) = textareaLocator.shouldHaveValue(value)

    fun shouldBeEnabled() = textareaLocator.shouldBeEnabled()

    fun shouldBeDisabled() = textareaLocator.shouldBeDisabled()

    fun shouldHavePreview() = previewLocator.shouldBeVisible()

    companion object {
        fun byContainer(container: Locator) = Markdown(container.locator(".sa-notes-input"))
    }
}
