package io.orangebuffalo.simpleaccounting.tests.ui.shared.pages

import com.microsoft.playwright.Page
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

class OAuthAuthorizationPopup(page: Page) : SaPageBase<OAuthAuthorizationPopup>(page) {
    private val pageContainer = page.locator(".oauth-callback-page")
    private val status = components.statusLabel()

    private fun shouldHaveLoadingState() {
        withHint("OAuth authorization popup should be loading") {
            pageContainer.shouldBeVisible()
            status.shouldBeRegular()
            pageContainer.reportRendering("shared.oauth-authorization-popup.loading")
        }
    }

    fun shouldHaveErrorState() {
        withHint("OAuth authorization popup should be showing error") {
            pageContainer.shouldBeVisible()
            status.shouldBeError()
            pageContainer.reportRendering("shared.oauth-authorization-popup.error")
        }
    }

    companion object {
        fun Page.shouldHaveAuthorizationPopupOpenBy(action: () -> Unit): OAuthAuthorizationPopup {
            var authorizationPopup: OAuthAuthorizationPopup? = null
            withBlockedApiResponse(
                "auth/oauth2/callback",
                initiator = {
                    val popup = waitForPopup(action)
                    authorizationPopup = OAuthAuthorizationPopup(popup)
                    withHint("OAuth authorization popup should be visible") {
                        authorizationPopup!!.pageContainer.shouldBeVisible()
                    }
                },
                blockedRequestSpec = {
                    authorizationPopup!!.shouldHaveLoadingState()
                }
            )
            return authorizationPopup.shouldNotBeNull()
        }

        /**
         * Configures the generated error ID to match the expected failure rendering result
         */
        fun TokenGenerator.setupErrorIdForOAuthAuthorizationFailure() {
            whenever(generateUuid()) doReturn "test-error-id"
        }
    }
}
