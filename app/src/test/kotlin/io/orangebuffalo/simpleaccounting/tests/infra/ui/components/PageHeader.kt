package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

class PageHeader private constructor(
    private val locator: Locator,
) : UiComponent<PageHeader>() {
    fun shouldBeVisible() {
        locator.shouldBeVisible()
    }

    companion object {
        fun ComponentsAccessors.pageHeader(text: String) =
            PageHeader(page.locator(XPath.h1WithText(text)))
    }
}
