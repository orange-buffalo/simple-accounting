package io.orangebuffalo.simpleaccounting.business.ui.shared.loginbylink

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.loginbylink.LoginByLinkPage.Companion.openLoginByLinkPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.loginbylink.LoginByLinkPage.Companion.shouldBeLoginByLinkPage
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.navigateAndDisableAnimations
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

class LoginByLinkFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display loading state and navigate to dashboard on success`(page: Page) {
        page.withBlockedGqlApiResponse(
            queryOrMutationName = "createAccessTokenByWorkspaceAccessToken",
            initiator = {
                page.navigateAndDisableAnimations("/login-by-link/${preconditions.validToken}")
                page.shouldBeLoginByLinkPage {
                    statusMessage {
                        shouldBeRegular("We are verifying your access token...")
                    }
                }
            },
            blockedRequestSpec = {
                page.shouldBeLoginByLinkPage {
                    statusMessage {
                        shouldBeRegular("We are verifying your access token...")
                    }
                    reportRendering("login-by-link.loading")
                }
            },
        )

        page.shouldBeLoginByLinkPage {
            statusMessage {
                shouldBeSuccess("Access granted. Redirecting to your workspace...")
            }
            reportRendering("login-by-link.success")
        }

        page.shouldBeDashboardPage()
    }

    @Test
    fun `should display error state for invalid token`(page: Page) {
        page.openLoginByLinkPage("invalid-token-value") {
            statusMessage {
                shouldBeError("The access token is not valid. Please request a new link.")
            }
            reportRendering("login-by-link.error")
        }
    }

    @Test
    fun `should display error state for expired token`(page: Page) {
        page.openLoginByLinkPage(preconditions.expiredToken) {
            statusMessage {
                shouldBeError("The access token is not valid. Please request a new link.")
            }
        }
    }

    @Test
    fun `should display error state for revoked token`(page: Page) {
        page.openLoginByLinkPage(preconditions.revokedToken) {
            statusMessage {
                shouldBeError("The access token is not valid. Please request a new link.")
            }
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val validToken = fry().let { fry ->
                val ws = workspace(owner = fry)
                workspaceAccessToken(
                    workspace = ws,
                    token = "valid-access-token",
                    validTill = MOCK_TIME.plusSeconds(86400),
                    timeCreated = MOCK_TIME,
                ).token
            }
            val expiredToken = zoidberg().let { zoidberg ->
                val ws = workspace(owner = zoidberg)
                workspaceAccessToken(
                    workspace = ws,
                    token = "expired-access-token",
                    validTill = MOCK_TIME.minusSeconds(1),
                    timeCreated = MOCK_TIME.minusSeconds(3600),
                ).token
            }
            val revokedToken = roberto().let { roberto ->
                val ws = workspace(owner = roberto)
                workspaceAccessToken(
                    workspace = ws,
                    token = "revoked-access-token",
                    validTill = MOCK_TIME.plusSeconds(86400),
                    timeCreated = MOCK_TIME,
                    revoked = true,
                ).token
            }
        }
    }
}
