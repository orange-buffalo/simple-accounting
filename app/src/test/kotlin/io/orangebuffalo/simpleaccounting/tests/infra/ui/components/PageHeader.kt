package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath

class PageHeader<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, PageHeader<P>>(parent) {
    fun shouldBeVisible(): P {
        locator.shouldBeVisible()
        return parent
    }

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.pageHeader(text: String) =
            PageHeader(page.locator(XPath.h1WithText(text)), this.owner)
    }
}
