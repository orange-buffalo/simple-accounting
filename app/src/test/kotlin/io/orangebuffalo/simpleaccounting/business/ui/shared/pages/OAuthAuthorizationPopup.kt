package io.orangebuffalo.simpleaccounting.business.ui.shared.pages

import com.microsoft.playwright.Page
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

class OAuthAuthorizationPopup private constructor(page: Page) : SaPageBase(page) {
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
            withBlockedGqlApiResponse(
                "completeOAuth2Flow",
                initiator = {
                    val popup = waitForPopup(action)
                    authorizationPopup = OAuthAuthorizationPopup(popup)
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
