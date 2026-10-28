package io.orangebuffalo.simpleaccounting.business.ui.user.integrations

import com.microsoft.playwright.Page
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByContainer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ConfirmationDialog.Companion.confirmationDialog
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Tooltip
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

class IntegrationsPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Integrations")
    val providers = ProviderCards(components)
    val status = components.statusLabel()
    val authenticationTooltip = Tooltip.byTrigger(page.locator(".integration-card .sa-status-label_failure"))
    val remove = components.buttonByContainer(page.locator(".integration-card"))
    val confirmation = components.confirmationDialog()

    fun reportRenderingWithPopovers(name: String) {
        page.locator("body").reportRendering(name)
    }

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

    @UiComponentMarker
    class ProviderCards(components: ComponentsAccessors) {
        private val container = components.page.locator(".integration-cards")

        fun setupWise() = container.getByRole(AriaRole.LINK, Locator.GetByRoleOptions().setName("Setup →").setExact(true)).click()
        fun updateWise() = container.getByRole(AriaRole.LINK, Locator.GetByRoleOptions().setName("Update →").setExact(true)).click()
        fun shouldHaveUpdateDisabled() {
            container.getByRole(AriaRole.LINK, Locator.GetByRoleOptions().setName("Update →").setExact(true)).shouldHaveCount(0)
            container.locator("span[aria-disabled=true]").shouldBeVisible()
        }
    }
}
