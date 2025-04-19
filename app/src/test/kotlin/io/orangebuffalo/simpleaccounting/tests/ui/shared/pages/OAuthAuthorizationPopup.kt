package io.orangebuffalo.simpleaccounting.tests.ui.shared.pages

import com.microsoft.playwright.Page
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.tests.infra.ui.assertRendering
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse

class OAuthAuthorizationPopup(page: Page) : SaPageBase<OAuthAuthorizationPopup>(page) {
    private val pageContainer = page.locator(".oauth-callback-page")
    private val status = components.statusLabel()

    private fun shouldHaveLoadingState() {
        withClue("OAuth authorization popup should be loading") {
            pageContainer.shouldBeVisible()
            status.shouldBeRegular()
            pageContainer.assertRendering("shared/oauth-authorization-popup/loading")
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
                    withClue("OAuth authorization popup should be visible") {
                        authorizationPopup!!.pageContainer.shouldBeVisible()
                    }
                },
                blockedRequestSpec = {
                    authorizationPopup!!.shouldHaveLoadingState()
                }
            )
            return authorizationPopup.shouldNotBeNull()
        }
    }
}
