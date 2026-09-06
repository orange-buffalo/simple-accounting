package io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewFilters.Companion.overviewFilters
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class OAuthProvidersOverviewPage private constructor(page: Page) : SaPageBase(page) {
    val pageItems = components.overviewItems()
    val filters = components.overviewFilters()
    private val header = components.pageHeader("Authentication Providers")
    val registerProviderButton = components.buttonByText("Register provider")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeOAuthProvidersOverviewPage(spec: OAuthProvidersOverviewPage.() -> Unit = {}) {
            OAuthProvidersOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openOAuthProvidersOverviewPage(spec: OAuthProvidersOverviewPage.() -> Unit) {
            navigate("/admin/oauth-providers")
            shouldBeOAuthProvidersOverviewPage(spec)
        }
    }
}
