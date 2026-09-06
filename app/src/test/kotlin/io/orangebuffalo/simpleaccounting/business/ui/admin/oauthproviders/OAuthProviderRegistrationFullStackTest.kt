package io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvider
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders.EditOAuthProviderPage.Companion.shouldBeEditOAuthProviderPage
import io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders.OAuthProvidersOverviewPage.Companion.openOAuthProvidersOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders.OAuthProvidersOverviewPage.Companion.shouldBeOAuthProvidersOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders.RegisterOAuthProviderPage.Companion.shouldBeRegisterOAuthProviderPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.OAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import org.junit.jupiter.api.Test

class OAuthProviderRegistrationFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should register a new provider and show it in the overview`(page: Page) {
        val testData = preconditions {
            object {
                val farnsworth = farnsworth()
            }
        }
        page.authenticateViaCookie(testData.farnsworth)

        page.openOAuthProvidersOverviewPage {
            registerProviderButton.click()
        }

        page.shouldBeRegisterOAuthProviderPage {
            callbackUrl {
                input.shouldHaveUrl("${simpleAccountingProperties.publicUrl}/oauth-identity-callback")
            }
            reportRendering("admin.oauth-providers.registration-form")

            name { input.fill("Nimbus Auth") }
            clientId { input.fill("nimbus-client-id") }
            clientSecret { input.fill("nimbus-client-secret") }
            authorizationUrl { input.fill("https://nimbus.example/authorize") }
            tokenUrl { input.fill("https://nimbus.example/token") }
            userInfoUrl { input.fill("https://nimbus.example/userinfo") }
            userIdAttribute { input.fill("email") }
            scopes { input.fill("openid email") }
            saveButton.click()
        }

        page.shouldBeEditOAuthProviderPage {
            shouldHaveNotifications {
                success("Provider Nimbus Auth has been successfully saved")
            }
        }

        withHint("Should store the provider") {
            val provider = aggregateTemplate.findAll<OAuthProvider>().shouldBeSingle()
            provider.name.shouldBe("Nimbus Auth")
            provider.clientId.shouldBe("nimbus-client-id")
            provider.clientSecret.shouldBe("nimbus-client-secret")
            provider.authorizationUrl.shouldBe("https://nimbus.example/authorize")
            provider.tokenUrl.shouldBe("https://nimbus.example/token")
            provider.userInfoUrl.shouldBe("https://nimbus.example/userinfo")
            provider.userIdAttribute.shouldBe("email")
            provider.scopes.map { it.scope }.shouldContainExactlyInAnyOrder("openid", "email")
        }

        page.openOAuthProvidersOverviewPage {
            pageItems { shouldHaveTitles("Nimbus Auth") }
        }
    }

    @Test
    fun `should discover OIDC settings before registering a provider`(page: Page) {
        val testData = preconditions {
            object {
                val farnsworth = farnsworth()
            }
        }
        page.authenticateViaCookie(testData.farnsworth)

        page.openOAuthProvidersOverviewPage {
            registerProviderButton.click()
        }

        val issuerUrl = OAuthMocks.issuerUrl("nimbus-discovery").toString()
        page.shouldBeRegisterOAuthProviderPage {
            providerBaseUrl { input.fill(issuerUrl) }
            loadProviderSettingsButton.click()

            authorizationUrl { input.shouldHaveValue("$issuerUrl/authorize") }
            tokenUrl { input.shouldHaveValue("$issuerUrl/token") }
            userInfoUrl { input.shouldHaveValue("$issuerUrl/userinfo") }
            userIdAttribute { input.shouldHaveValue("sub") }
            scopes { input.shouldHaveValue("openid") }

            name { input.fill("Discovered Auth") }
            clientId { input.fill("discovered-client-id") }
            clientSecret { input.fill("discovered-client-secret") }
            saveButton.click()
        }

        page.shouldBeEditOAuthProviderPage {
            shouldHaveNotifications {
                success("Provider Discovered Auth has been successfully saved")
            }
        }

        val provider = aggregateTemplate.findAll<OAuthProvider>().shouldBeSingle()
        provider.authorizationUrl.shouldBe("$issuerUrl/authorize")
        provider.tokenUrl.shouldBe("$issuerUrl/token")
        provider.userInfoUrl.shouldBe("$issuerUrl/userinfo")
        provider.userIdAttribute.shouldBe("sub")
        provider.scopes.map { it.scope }.shouldContainExactlyInAnyOrder("openid")
    }

    @Test
    fun `should report duplicate provider names`(page: Page) {
        val testData = preconditions {
            object {
                val farnsworth = farnsworth()

                init {
                    oauthProvider(name = "Nimbus Auth")
                }
            }
        }
        page.authenticateViaCookie(testData.farnsworth)

        page.openOAuthProvidersOverviewPage {
            registerProviderButton.click()
        }

        page.shouldBeRegisterOAuthProviderPage {
            name { input.fill("Nimbus Auth") }
            clientId { input.fill("another-client-id") }
            clientSecret { input.fill("another-client-secret") }
            authorizationUrl { input.fill("https://nimbus.example/authorize") }
            tokenUrl { input.fill("https://nimbus.example/token") }
            userInfoUrl { input.fill("https://nimbus.example/userinfo") }
            saveButton.click()

            name { shouldHaveValidationError("Provider with name \"Nimbus Auth\" already exists") }
            shouldHaveNotifications { validationFailed() }
        }

        aggregateTemplate.findAll<OAuthProvider>().shouldBeSingle()
    }

    @Test
    fun `should edit a provider keeping the client secret when it is not re-entered`(page: Page) {
        val testData = preconditions {
            object {
                val farnsworth = farnsworth()
                val nimbus = oauthProvider(
                    name = "Nimbus Auth",
                    clientId = "nimbus-client-id",
                    clientSecret = "nimbus-client-secret",
                    authorizationUrl = "https://nimbus.example/authorize",
                    tokenUrl = "https://nimbus.example/token",
                    userInfoUrl = "https://nimbus.example/userinfo",
                    userIdAttribute = "sub",
                    scopes = setOf("openid"),
                )
            }
        }
        page.authenticateViaCookie(testData.farnsworth)

        page.openOAuthProvidersOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "Nimbus Auth" }.executeEditAction()
            }
        }

        page.shouldBeEditOAuthProviderPage {
            name { input.shouldHaveValue("Nimbus Auth") }
            clientId { input.shouldHaveValue("nimbus-client-id") }
            clientSecret { input.shouldHaveValue("") }
            authorizationUrl { input.shouldHaveValue("https://nimbus.example/authorize") }
            tokenUrl { input.shouldHaveValue("https://nimbus.example/token") }
            userInfoUrl { input.shouldHaveValue("https://nimbus.example/userinfo") }
            userIdAttribute { input.shouldHaveValue("sub") }
            scopes { input.shouldHaveValue("openid") }
            reportRendering("admin.oauth-providers.edit-form")

            name { input.fill("Nimbus SSO") }
            scopes { input.fill("openid profile") }
            saveButton.click()
        }

        page.shouldBeOAuthProvidersOverviewPage {
            shouldHaveNotifications {
                success("Provider Nimbus SSO has been successfully saved")
            }
        }

        withHint("Should update the provider and keep the secret") {
            val provider = aggregateTemplate.findSingle<OAuthProvider>(testData.nimbus.id!!)
            provider.name.shouldBe("Nimbus SSO")
            provider.clientSecret.shouldBe("nimbus-client-secret")
            provider.scopes.map { it.scope }.shouldContainExactlyInAnyOrder("openid", "profile")
        }
    }
}
