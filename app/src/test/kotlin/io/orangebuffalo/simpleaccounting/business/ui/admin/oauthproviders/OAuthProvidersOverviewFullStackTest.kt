package io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders.OAuthProvidersOverviewPage.Companion.openOAuthProvidersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaActionLink
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaIconType
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItemData
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test

class OAuthProvidersOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should provide the registered providers overview`(page: Page) {
        val testData = preconditions {
            object {
                val farnsworth = farnsworth()

                init {
                    oauthProvider(
                        name = "Nimbus Auth",
                        clientId = "nimbus-client-id",
                        userIdAttribute = "sub",
                        scopes = setOf("openid", "email"),
                        createdAt = MOCK_TIME.plusSeconds(1),
                    )
                    oauthProvider(
                        name = "MomCorp ID",
                        clientId = "momcorp-client-id",
                        userIdAttribute = "email",
                        scopes = setOf(),
                        createdAt = MOCK_TIME.plusSeconds(2),
                    )
                }
            }
        }
        page.authenticateViaCookie(testData.farnsworth)
        page.openOAuthProvidersOverviewPage {
            pageItems {
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "MomCorp ID",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.LOGIN, "momcorp-client-id"),
                            primaryAttribute(SaIconType.PROFILE, "email"),
                        ),
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Nimbus Auth",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.LOGIN, "nimbus-client-id"),
                            primaryAttribute(SaIconType.PROFILE, "sub"),
                            primaryAttribute(SaIconType.GEAR, "email openid"),
                        ),
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                )
            }
            reportRendering("admin.oauth-providers.overview")
        }
    }

    @Test
    fun `should support filtering by provider name`(page: Page) {
        val testData = preconditions {
            object {
                val farnsworth = farnsworth()

                init {
                    oauthProvider(name = "Nimbus Auth", createdAt = MOCK_TIME.plusSeconds(1))
                    oauthProvider(name = "MomCorp ID", createdAt = MOCK_TIME.plusSeconds(2))
                    oauthProvider(name = "Globetrotters SSO", createdAt = MOCK_TIME.plusSeconds(3))
                }
            }
        }
        page.authenticateViaCookie(testData.farnsworth)
        page.openOAuthProvidersOverviewPage {
            pageItems {
                shouldHaveTitles("Globetrotters SSO", "MomCorp ID", "Nimbus Auth")
            }
            filters.addTextFilter("Provider name", "mom")
            pageItems {
                shouldHaveTitles("MomCorp ID")
            }
        }
    }

    @Test
    fun `should show empty overview when no providers are registered`(page: Page) {
        val testData = preconditions {
            object {
                val farnsworth = farnsworth()
            }
        }
        page.authenticateViaCookie(testData.farnsworth)
        page.openOAuthProvidersOverviewPage {
            pageItems {
                shouldHaveTitles(emptyList())
            }
            registerProviderButton.shouldBeVisible()
            reportRendering("admin.oauth-providers.empty-overview")
        }
    }
}
