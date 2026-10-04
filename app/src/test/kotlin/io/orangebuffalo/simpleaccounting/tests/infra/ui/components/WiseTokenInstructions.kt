package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveAttribute
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

class WiseTokenInstructions private constructor(private val container: Locator) : UiComponent<WiseTokenInstructions>() {
    fun shouldExplainReadOnlyToken() {
        val steps = container.locator("ol li")
        steps.shouldHaveCount(4)
        listOf(
            "Sign in to your Wise account on wise.com.",
            "Open Your Account → Connect and manage apps → API tokens → Add new token.",
            "Choose read-only access when creating your personal token. Do not grant transfer or payment permissions.",
            "Complete two-step verification and copy the token. Wise displays it only once. Paste it below.",
        ).forEachIndexed { index, text -> steps.nth(index).shouldHaveText(text) }
        container.locator("p").last().shouldHaveText(
            "Use a dedicated read-only token. It is stored on the server and never displayed again. You can revoke it in Wise at any time."
        )
        container.locator("a").shouldHaveAttribute("href", "https://docs.wise.com/guides/developer/auth-and-security/personal-api-token")
    }

    companion object {
        fun ComponentsAccessors.wiseTokenInstructions() = WiseTokenInstructions(page.locator(".integration-wizard__content"))
    }
}
