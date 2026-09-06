package io.orangebuffalo.simpleaccounting.business.ui.shared.login

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthAuthenticationRequest
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.openLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.OAuthIdentityCallbackPage.Companion.shouldBeOAuthIdentityCallbackPage
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.UserAuthOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.mockedOAuthProvider
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import org.junit.jupiter.api.Test

/**
 * Verifies the second step of the login process when the user has linked an identity
 * at one of the registered OAuth2 providers.
 */
class OAuthLoginFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should login with the linked provider instead of the password`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace().also {
                    userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                }
            }
        }
        UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

        page.openLoginPage {
            submitUserName(testData.fry.userName)

            withHint("Password login should not be offered for users with linked identities") {
                passwordInput.shouldBeHidden()
                loginButton.shouldBeHidden()
            }
            rememberMeCheckbox { shouldBeChecked() }
            reportRendering("login.oauth-methods")

            oauthLoginButton("Nimbus Auth").click()
        }

        page.shouldBeDashboardPage()

        withHint("Should issue a refresh token as remember me was checked") {
            aggregateTemplate.findAll<RefreshToken>()
                .shouldBeSingle()
                .userId.shouldBe(testData.fry.id)
        }

        withHint("Should consume the pending authorization request") {
            aggregateTemplate.findAll<OAuthAuthenticationRequest>().shouldBeEmpty()
        }
    }

    @Test
    fun `should report a failure when the provider authenticates another identity`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace().also {
                    userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                }
            }
        }
        UserAuthOAuthMocks.mockIdentity("zapp-at-nimbus")

        page.openLoginPage {
            submitUserName(testData.fry.userName)
            oauthLoginButton("Nimbus Auth").click()
        }

        page.shouldBeOAuthIdentityCallbackPage {
            shouldHaveErrorMessage("The account at the provider is not the one linked to this user.")
            reportFailureRendering("login.oauth-identity-mismatch")
            backToLoginButton.click()
        }

        page.shouldBeLoginPage {}

        withHint("Should not authenticate the user") {
            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
        }
    }

    @Test
    fun `should offer all linked providers sorted by name`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val momCorp = oauthProvider(name = "MomCorp ID")
                val fry = fry().withWorkspace().also {
                    userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                    userOAuthIdentity(user = it, provider = momCorp, externalId = "fry-at-momcorp")
                }
            }
        }

        page.openLoginPage {
            submitUserName(testData.fry.userName)
            shouldHaveOAuthProviders("Continue with MomCorp ID", "Continue with Nimbus Auth")
            reportRendering("login.oauth-multiple-providers")
        }
    }

    @Test
    fun `should not issue a refresh token when remember me is disabled`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace().also {
                    userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                }
            }
        }
        UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

        page.openLoginPage {
            submitUserName(testData.fry.userName)
            rememberMeCheckbox {
                shouldBeChecked()
                click()
                shouldNotBeChecked()
            }
            oauthLoginButton("Nimbus Auth").click()
        }

        page.shouldBeDashboardPage()

        withHint("Should not persist a session the user has not asked for") {
            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
            page.context().cookies().find { it.name == "refreshToken" }.shouldBeNull()
        }
    }

    @Test
    fun `should submit both login steps with the keyboard`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.openLoginPage {
            loginInput { fill(testData.fry.userName) }
            loginInput { pressEnter() }
            passwordInput { fill(testData.fry.passwordHash) }
            passwordInput { pressEnter() }
        }

        page.shouldBeDashboardPage()
    }

    @Test
    fun `should not proceed with a blank username`(page: Page) {
        preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.openLoginPage {
            loginInput { fill("   ") }
            continueButton { shouldBeDisabled() }
            loginInput { pressEnter() }
            continueButton { shouldBeVisible() }
            passwordInput { shouldBeHidden() }
        }
    }

    @Test
    fun `should offer recovery when the callback and the session recovery both fail`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace().also {
                    userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                }
            }
        }
        UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

        try {
            page.openLoginPage {
                submitUserName(testData.fry.userName)

                // emulates the API becoming unavailable while the user is at the authorization
                // server: both the callback and the session recovery it falls back to fail
                page.context().route("**/api/graphql") { route ->
                    val body = route.request().postData() ?: ""
                    if (body.contains("completeOAuthAuthentication") || body.contains("refreshAccessToken")) {
                        route.abort()
                    } else {
                        route.resume()
                    }
                }

                oauthLoginButton("Nimbus Auth").click()
            }

            page.shouldBeOAuthIdentityCallbackPage {
                shouldHaveErrorMessage(
                    "Authentication failed. Please try again or contact your administrator."
                )
                shouldOfferRecoveryTo("Back to login")
                reportFailureRendering("login.oauth-callback-unavailable")
            }
        } finally {
            page.context().unroute("**/api/graphql")
        }
    }

    @Test
    fun `should offer password login when the user has not linked any identity`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().withWorkspace()
            }.also {
                mockedOAuthProvider(name = "Nimbus Auth")
            }
        }

        page.openLoginPage {
            submitUserName(testData.fry.userName)

            withHint("Registered providers the user is not linked to should not be offered") {
                oauthLoginButton("Nimbus Auth").shouldBeHidden()
            }
            passwordInput { fill(testData.fry.passwordHash) }
            loginButton { click() }
        }

        page.shouldBeDashboardPage()
    }
}
