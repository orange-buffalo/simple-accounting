package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.booleans.shouldBeTrue
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeHidden
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

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

    fun shouldNotBeVisible() = locator.shouldBeHidden()

    fun shouldHaveLabelSatisfying(spec: (String) -> Unit) = locator.shouldSatisfy {
        spec(this.innerText())
    }

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.buttonByText(label: String) =
            Button(page.locator("xpath=//button[${XPath.hasText(label)}]"), this.owner)

        /**
         * Assumes a single button in the container and returns it
         */
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.buttonByContainer(container: Locator) =
            Button(container.locator("button"), this.owner)
    }
}
