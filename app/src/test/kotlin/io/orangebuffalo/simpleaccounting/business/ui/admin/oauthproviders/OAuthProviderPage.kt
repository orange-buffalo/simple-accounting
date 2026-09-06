package io.orangebuffalo.simpleaccounting.business.ui.admin.oauthproviders

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByContainer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponent

abstract class OAuthProviderPageBase(page: Page) : SaPageBase(page) {
    val providerBaseUrl = components.formItemTextInputByLabel("Provider base URL")
    val loadProviderSettingsButton = components.buttonByText("Load provider settings")
    val name = components.formItemTextInputByLabel("Provider name")
    val clientId = components.formItemTextInputByLabel("Client ID")
    val clientSecret = components.formItemTextInputByLabel("Client secret")
    val authorizationUrl = components.formItemTextInputByLabel("Authorization endpoint")
    val tokenUrl = components.formItemTextInputByLabel("Token endpoint")
    val userInfoUrl = components.formItemTextInputByLabel("User info endpoint")
    val userIdAttribute = components.formItemTextInputByLabel("User ID attribute")
    val scopes = components.formItemTextInputByLabel("Scopes")
    val callbackUrl = components.formItemByLabel("Redirect URL to whitelist at the provider") {
        CallbackUrl(it, components)
    }
    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")
}

class RegisterOAuthProviderPage private constructor(page: Page) : OAuthProviderPageBase(page) {
    private val header = components.pageHeader("Register Authentication Provider")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeRegisterOAuthProviderPage(spec: RegisterOAuthProviderPage.() -> Unit) {
            RegisterOAuthProviderPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}

class EditOAuthProviderPage private constructor(page: Page) : OAuthProviderPageBase(page) {
    private val header = components.pageHeader("Edit Authentication Provider")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditOAuthProviderPage(spec: EditOAuthProviderPage.() -> Unit) {
            EditOAuthProviderPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}

class CallbackUrl(
    container: Locator,
    components: ComponentsAccessors,
) : UiComponent<CallbackUrl>() {

    private val link = components.buttonByContainer(container)

    fun shouldHaveUrl(expectedUrl: String) {
        link.shouldHaveLabelSatisfying { label -> label.shouldBe(expectedUrl) }
    }

    fun copyToClipboard() {
        link.click()
    }
}
