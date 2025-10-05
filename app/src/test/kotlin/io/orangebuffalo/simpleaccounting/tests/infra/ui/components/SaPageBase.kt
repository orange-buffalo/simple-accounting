package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Page

@UiComponentMarker
open class SaPageBase(protected val page: Page) {
    protected val components: ComponentsAccessors = ComponentsAccessors(page)

    @UiComponentMarker
    fun shouldHaveNotifications(spec: Notifications.() -> Unit) {
        Notifications(page).spec()
    }
}
