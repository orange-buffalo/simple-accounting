package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class PageHeader<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, PageHeader<P>>(parent) {
    fun shouldBeVisible(): P {
        locator.assertThat().isVisible()
        return parent
    }

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.pageHeader(text: String) =
            PageHeader(page.locator(XPath.h1WithText(text)), this.owner)
    }
}
