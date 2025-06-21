package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Page

@Suppress("UNCHECKED_CAST", "LeakingThis")
open class SaPageBase<T : SaPageBase<T>>(private val page: Page) {
    protected val components: ComponentsAccessors<T> = ComponentsAccessors(page, this as T)

    fun shouldHaveNotifications(spec: Notifications.() -> Unit): T {
        Notifications(page).spec()
        return this as T
    }

    /**
     * Supports fluent definitions on this page when additional actions are
     * needed to be executed in between components interactions.
     */
    fun then(action: () -> Unit): T {
        action()
        return this as T
    }
}
