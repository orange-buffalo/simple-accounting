package io.orangebuffalo.simpleaccounting.web.ui.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class DashboardPage(private val page: Page) {
    private val header = page.locator("//h1[normalize-space(.) = 'Dashboard']")
    fun shouldBeOpen(): DashboardPage {
        header.assertThat().isVisible()
        return this
    }
}
