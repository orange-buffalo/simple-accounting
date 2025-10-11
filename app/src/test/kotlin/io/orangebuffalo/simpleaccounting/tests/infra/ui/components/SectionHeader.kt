package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

/**
 * Component representing an h2 section header on a page.
 * Use this to verify visibility of different sections or to check language-specific headers.
 */
class SectionHeader private constructor(
    private val locator: Locator,
) : UiComponent<SectionHeader>() {
    fun shouldBeVisible() {
        locator.shouldBeVisible()
    }

    fun shouldBeHidden() {
        locator.shouldBeHidden()
    }

    companion object {
        fun ComponentsAccessors.sectionHeader(text: String) =
            SectionHeader(page.locator("//h2[${XPath.hasText(text)}]"))
    }
}
