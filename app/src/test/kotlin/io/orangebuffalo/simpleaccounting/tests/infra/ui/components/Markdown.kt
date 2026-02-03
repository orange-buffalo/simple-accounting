package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveValue
import io.orangebuffalo.kotestplaywrightassertions.shouldContainText

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

    fun shouldNotHavePreview() = previewLocator.shouldBeHidden()

    fun shouldHavePreviewWithHeading(text: String) {
        previewLocator.shouldBeVisible()
        previewLocator.getByRole(com.microsoft.playwright.options.AriaRole.HEADING).filter(
            com.microsoft.playwright.Locator.FilterOptions().setHasText(text)
        ).shouldBeVisible()
    }

    companion object {
        fun byContainer(container: Locator) = Markdown(container)
    }
}
