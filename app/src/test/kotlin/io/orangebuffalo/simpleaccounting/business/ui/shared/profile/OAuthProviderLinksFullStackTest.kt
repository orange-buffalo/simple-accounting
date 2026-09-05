package io.orangebuffalo.simpleaccounting.business.ui.shared.profile

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.oauthproviders.UserOAuthIdentity
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.openLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.shouldBeLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.OAuthIdentityCallbackPage.Companion.shouldBeOAuthIdentityCallbackPage
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.UserAuthOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.mockedOAuthProvider
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import org.junit.jupiter.api.Test

/**
 * Verifies linking and unlinking of the OAuth2 provider identities on My Profile page.
 * See also [io.orangebuffalo.simpleaccounting.business.ui.shared.login.OAuthLoginFullStackTest]
 * for logging in with a linked identity.
 */
class OAuthProviderLinksFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should link an identity at the provider and then unlink it`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace()
            }
        }
        UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")
        page.authenticateViaCookie(testData.fry)

        page.openMyProfilePage {
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) {
                    shouldHaveName("Nimbus Auth")
                    shouldNotBeLinked()
                }
                reportRendering("profile.oauth-providers.not-linked")
                provider(testData.nimbus.id!!) { clickAction() }
            }
        }

        page.shouldBeMyProfilePage {
            shouldHaveNotifications {
                success("Your account has been linked to the authentication provider")
            }
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) {
                    shouldBeLinkedAs("fry-at-nimbus")
                }
                reportRendering("profile.oauth-providers.linked")
            }
        }

        withHint("Should store the linked identity") {
            val identity = aggregateTemplate.findAll<UserOAuthIdentity>().shouldBeSingle()
            identity.userId.shouldBe(testData.fry.id)
            identity.providerId.shouldBe(testData.nimbus.id)
            identity.externalId.shouldBe("fry-at-nimbus")
        }

        page.openMyProfilePage {
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) { clickAction() }
            }
            shouldHaveUnlinkConfirmation(
                "Password login will become available again for this account. Continue?"
            ) { clickButton("Unlink") }
            shouldHaveNotifications {
                success("The provider has been unlinked from your account")
            }
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) { shouldNotBeLinked() }
            }
        }

        withHint("Should remove the linked identity") {
            aggregateTemplate.findAll<UserOAuthIdentity>().shouldBeEmpty()
        }
    }

    @Test
    fun `should report a failure when the identity belongs to another user`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace()
                val bender = bender().also {
                    userOAuthIdentity(user = it, provider = nimbus, externalId = "shared-identity")
                }
            }
        }
        UserAuthOAuthMocks.mockIdentity("shared-identity")
        page.authenticateViaCookie(testData.fry)

        page.openMyProfilePage {
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) { clickAction() }
            }
        }

        page.shouldBeOAuthIdentityCallbackPage {
            shouldHaveErrorMessage("The account at the provider is already linked to another user.")
            reportFailureRendering("profile.oauth-providers.identity-already-in-use")
        }

        withHint("Should not link the identity") {
            aggregateTemplate.findAll<UserOAuthIdentity>()
                .shouldBeSingle()
                .userId.shouldBe(testData.bender.id)
        }
    }

    @Test
    fun `should keep the identity when unlinking is cancelled`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace().also {
                    userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                }
            }
        }
        page.authenticateViaCookie(testData.fry)

        page.openMyProfilePage {
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) { clickAction() }
            }
            shouldHaveUnlinkConfirmation(
                "Password login will become available again for this account. Continue?"
            ) { clickButton("Cancel") }
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) { shouldBeLinkedAs("fry-at-nimbus") }
            }
        }

        withHint("Should keep the identity linked") {
            aggregateTemplate.findAll<UserOAuthIdentity>().shouldBeSingle()
        }
    }

    @Test
    fun `should hide password management while an identity is linked`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace().also {
                    userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                }
            }
        }
        page.authenticateViaCookie(testData.fry)

        page.openMyProfilePage {
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) { shouldBeLinkedAs("fry-at-nimbus") }
            }
            shouldHavePasswordChangeSectionHidden()
        }
    }

    @Test
    fun `should keep password login unavailable while another identity remains linked`(page: Page) {
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
        page.authenticateViaCookie(testData.fry)

        page.openMyProfilePage {
            shouldHavePasswordChangeSectionHidden()
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.momCorp.id!!) { clickAction() }
            }
            shouldHaveUnlinkConfirmation(
                "This provider will no longer be usable to login. Continue?"
            ) { clickButton("Unlink") }
            shouldHaveNotifications {
                success("The provider has been unlinked from your account")
            }

            withHint("The remaining identity still disables password management") {
                shouldHavePasswordChangeSectionHidden()
                shouldHaveOAuthProvidersSectionVisible {
                    provider(testData.nimbus.id!!) { shouldBeLinkedAs("fry-at-nimbus") }
                    provider(testData.momCorp.id!!) { shouldNotBeLinked() }
                }
            }
        }

        page.openMyProfilePage {
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) { clickAction() }
            }
            shouldHaveUnlinkConfirmation(
                "Password login will become available again for this account. Continue?"
            ) { clickButton("Unlink") }
            shouldHaveNotifications {
                success("The provider has been unlinked from your account")
            }

            withHint("Password management returns once the last identity is removed") {
                shouldHavePasswordChangeSectionVisible()
            }
        }

        aggregateTemplate.findAll<UserOAuthIdentity>().shouldBeEmpty()
    }

    @Test
    fun `should take the user to login after linking without a persistent session`(page: Page) {
        val testData = preconditions {
            object {
                val nimbus = mockedOAuthProvider(name = "Nimbus Auth")
                val fry = fry().withWorkspace()
            }
        }
        UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")

        // no refresh token is issued, so the session cannot survive the redirects to the provider
        page.openLoginPage {
            submitUserName(testData.fry.userName)
            rememberMeCheckbox {
                shouldBeChecked()
                click()
                shouldNotBeChecked()
            }
            passwordInput { fill(testData.fry.passwordHash) }
            loginButton { click() }
        }
        page.shouldBeDashboardPage()

        // navigating within the application keeps the in-memory session that a page load would lose
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage {
            shouldHaveOAuthProvidersSectionVisible {
                provider(testData.nimbus.id!!) { clickAction() }
            }
        }

        withHint("The linking still succeeds, the browser session is simply gone") {
            page.shouldBeLoginPage {}
            aggregateTemplate.findAll<UserOAuthIdentity>()
                .shouldBeSingle()
                .externalId.shouldBe("fry-at-nimbus")
        }

        UserAuthOAuthMocks.mockIdentity("fry-at-nimbus")
        page.shouldBeLoginPage {
            submitUserName(testData.fry.userName)
            withHint("The freshly linked provider is the only way back in") {
                passwordInput.shouldBeHidden()
                oauthLoginButton("Nimbus Auth").click()
            }
        }

        page.shouldBeDashboardPage()
    }

    @Test
    fun `should hide the section when no providers are registered`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }
        page.authenticateViaCookie(testData.fry)

        page.openMyProfilePage {
            shouldHavePasswordChangeSectionVisible()
            shouldHaveOAuthProvidersSectionHidden()
        }
    }
}
