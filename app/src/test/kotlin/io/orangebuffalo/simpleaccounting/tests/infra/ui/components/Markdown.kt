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

    fun fill(text: String) = textareaLocator.fill(text)

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
        rootLocator.shouldBeVisible()
        rootLocator.shouldContainText(text)
    }

    fun shouldContainHeading(text: String) {
        rootLocator.locator("h1:has-text('$text'), h2:has-text('$text'), h3:has-text('$text'), h4:has-text('$text'), h5:has-text('$text'), h6:has-text('$text')").shouldBeVisible()
    }

    fun shouldContainLink(text: String) {
        rootLocator.locator("a:has-text('$text')").shouldBeVisible()
    }

    fun shouldContainBold(text: String) {
        rootLocator.locator("strong:has-text('$text')").shouldBeVisible()
    }

    fun shouldContainItalic(text: String) {
        rootLocator.locator("em:has-text('$text')").shouldBeVisible()
    }

    fun shouldContainListItem(text: String) {
        rootLocator.locator("li:has-text('$text')").shouldBeVisible()
    }
}
