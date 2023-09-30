package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Page

@Suppress("UNCHECKED_CAST", "LeakingThis")
open class SaPageBase<T : SaPageBase<T>>(private val page: Page) {
    protected val components: ComponentsAccessors<T> = ComponentsAccessors(page, this as T)

    fun shouldHaveNotifications(spec: Notifications.() -> Unit): T {
        Notifications(page).spec()
        return this as T
    }
}
