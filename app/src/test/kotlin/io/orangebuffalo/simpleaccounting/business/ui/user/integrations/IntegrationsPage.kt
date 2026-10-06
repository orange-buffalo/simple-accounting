package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel

class IntegrationsPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Integrations")
    val providers = ProviderCards(page.locator(".integration-cards"))
    val status = components.statusLabel()

    companion object {
        fun Page.openIntegrationsPage(spec: IntegrationsPage.() -> Unit = {}) {
            navigate("/settings/integrations")
            shouldBeIntegrationsPage(spec)
        }

        fun Page.shouldBeIntegrationsPage(spec: IntegrationsPage.() -> Unit = {}) {
            IntegrationsPage(this).apply {
                header.shouldBeVisible()
                spec()
            }
        }
    }

    class ProviderCards(private val container: Locator) {
        fun setupWise() = container.getByRole(AriaRole.LINK, Locator.GetByRoleOptions().setName("Setup →").setExact(true)).click()
        fun viewWise() = container.getByRole(AriaRole.LINK, Locator.GetByRoleOptions().setName("View →").setExact(true)).click()
    }
}
