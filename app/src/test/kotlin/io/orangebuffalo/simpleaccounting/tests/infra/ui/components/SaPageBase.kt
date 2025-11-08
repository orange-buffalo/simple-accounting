package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

@UiComponentMarker
open class SaPageBase(protected val page: Page, pageContainerSelector: String = ".content-panel") {
    protected val components: ComponentsAccessors = ComponentsAccessors(page)
    protected val container = page.locator(pageContainerSelector)

    fun shouldHaveNotifications(spec: Notifications.() -> Unit) {
        Notifications(page).spec()
    }

    fun reportRendering(name: String) {
        container.reportRendering(name)
    }
}
