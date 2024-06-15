package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.booleans.shouldBeTrue
import io.orangebuffalo.simpleaccounting.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.infra.utils.shouldBeVisible
import io.orangebuffalo.simpleaccounting.infra.utils.shouldNotBeVisible
import io.orangebuffalo.simpleaccounting.infra.utils.shouldSatisfy

class Button<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, Button<P>>(parent) {
    fun shouldBeDisabled() = locator.shouldSatisfy {
        this.isDisabled.shouldBeTrue()
    }

    fun shouldBeEnabled() = locator.shouldSatisfy {
        this.isEnabled.shouldBeTrue()
    }

    fun click() = locator.click()

    fun shouldBeVisible() = locator.shouldBeVisible()

    fun shouldNotBeVisible() = locator.shouldNotBeVisible()

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.buttonByText(label: String) =
            Button(page.locator("xpath=//button[${XPath.hasText(label)}]"), this.owner)
    }
}
