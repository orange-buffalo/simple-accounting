package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Page

open class SaPageBase<T : SaPageBase<T>>(page: Page) {
    @Suppress("UNCHECKED_CAST", "LeakingThis")
    protected val components: ComponentsAccessors<T> = ComponentsAccessors(page, this as T)
}
