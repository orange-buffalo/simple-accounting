package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

class Button<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, Button<P>>(parent) {
    fun shouldBeDisabled() = locator.shouldBeDisabled()

    fun shouldBeEnabled() = locator.shouldBeEnabled()

    fun click() = locator.click()

    fun shouldBeVisible() = locator.shouldBeVisible()

    fun shouldBeHidden() = locator.shouldBeHidden()

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
