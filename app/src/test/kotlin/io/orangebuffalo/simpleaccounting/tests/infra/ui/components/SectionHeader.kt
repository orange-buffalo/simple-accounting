package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

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
