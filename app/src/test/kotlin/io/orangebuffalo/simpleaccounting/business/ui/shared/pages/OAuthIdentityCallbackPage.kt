package io.orangebuffalo.simpleaccounting.business.ui.shared.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

/**
 * The page the OAuth2 providers redirect the users back to. On success it takes the user further,
 * so it is only observable in the failure scenarios.
 */
class OAuthIdentityCallbackPage private constructor(page: Page) :
    SaPageBase(page, ".oauth-identity-callback-page") {

    private val status = components.statusLabel(container)
    val backToLoginButton = components.buttonByText("Back to login")

    private fun shouldBeOpen() {
        container.shouldBeVisible()
    }

    fun shouldHaveErrorMessage(message: String) {
        status.shouldBeError(message)
    }

    fun shouldOfferRecoveryTo(actionLabel: String) {
        components.buttonByText(actionLabel).shouldBeVisible()
    }

    fun reportFailureRendering(name: String) {
        container.reportRendering(name)
    }

    companion object {
        fun Page.shouldBeOAuthIdentityCallbackPage(spec: OAuthIdentityCallbackPage.() -> Unit) {
            OAuthIdentityCallbackPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
