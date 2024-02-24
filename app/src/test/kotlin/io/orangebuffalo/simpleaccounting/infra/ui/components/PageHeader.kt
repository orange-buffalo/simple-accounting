package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class PageHeader<T : Any> private constructor(
    private val locator: Locator,
    private val parent: T,
) {
    fun shouldBeVisible(): T {
        locator.assertThat().isVisible()
        return parent
    }

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.pageHeader(text: String) =
            PageHeader(page.locator(XPath.h1WithText(text)), this.owner)
    }
}
