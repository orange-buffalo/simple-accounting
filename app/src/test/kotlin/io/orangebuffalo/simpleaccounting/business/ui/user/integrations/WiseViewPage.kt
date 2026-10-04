package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class WiseViewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Wise integration")

    companion object {
        fun Page.shouldBeWiseViewPage() {
            WiseViewPage(this).header.shouldBeVisible()
        }
    }
}
