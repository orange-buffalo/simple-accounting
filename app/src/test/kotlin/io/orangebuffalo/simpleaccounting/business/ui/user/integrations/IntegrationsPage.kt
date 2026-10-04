package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.IntegrationCards.Companion.integrationCards
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel

class IntegrationsPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Integrations")
    val providers = components.integrationCards()
    val status = components.statusLabel()

    companion object {
        fun Page.shouldBeIntegrationsPage(spec: IntegrationsPage.() -> Unit = {}) {
            IntegrationsPage(this).apply {
                header.shouldBeVisible()
                spec()
            }
        }
    }
}
