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
    private val previewContentLocator = previewLocator.locator(".markdown-output")

    fun fill(text: String) {
        textareaLocator.fill(text)
        // Trigger input event to ensure Vue reactivity picks up the change
        textareaLocator.dispatchEvent("input")
    }

    fun shouldBeVisible() = textareaLocator.shouldBeVisible()

    fun shouldHaveValue(value: String) = textareaLocator.shouldHaveValue(value)

    fun shouldBeEnabled() = textareaLocator.shouldBeEnabled()

    fun shouldBeDisabled() = textareaLocator.shouldBeDisabled()

    fun shouldHavePreview() = previewLocator.shouldBeVisible()

    fun shouldNotHavePreview() = previewLocator.shouldBeHidden()

    fun shouldHavePreviewContaining(text: String) {
        previewContentLocator.shouldBeVisible()
        previewContentLocator.shouldContainText(text)
    }

    fun preview() = MarkdownPreview(previewContentLocator)

    companion object {
        fun byContainer(container: Locator) = Markdown(container)
    }
}

class MarkdownPreview internal constructor(
    private val rootLocator: Locator,
) {
    fun shouldContainText(text: String) {
        // Wait for the markdown container to have non-empty content
        rootLocator.locator("div:not(:empty)").first().waitFor()
        rootLocator.shouldContainText(text)
    }

    fun shouldContainHeading(text: String) {
        // Wait for any heading to appear, then check for the specific one
        rootLocator.locator("h1, h2, h3, h4, h5, h6").first().waitFor()
        rootLocator.shouldContainText(text)
    }

    fun shouldContainLink(text: String) {
        rootLocator.locator("a").first().waitFor()
        rootLocator.shouldContainText(text)
    }

    fun shouldContainBold(text: String) {
        rootLocator.locator("strong").first().waitFor()
        rootLocator.shouldContainText(text)
    }

    fun shouldContainItalic(text: String) {
        rootLocator.locator("em").first().waitFor()
        rootLocator.shouldContainText(text)
    }

    fun shouldContainListItem(text: String) {
        rootLocator.locator("li").first().waitFor()
        rootLocator.shouldContainText(text)
    }
}
