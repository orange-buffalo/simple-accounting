package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

class Button private constructor(
    private val locator: Locator,
) : UiComponent<Button>() {
    fun shouldBeDisabled() = locator.shouldBeDisabled()

    fun shouldBeEnabled() = locator.shouldBeEnabled()

    fun click() = locator.click()

    fun shouldBeVisible() = locator.shouldBeVisible()

    fun shouldBeHidden() = locator.shouldBeHidden()

    fun shouldHaveLabelSatisfying(spec: (String) -> Unit) = locator.shouldSatisfy {
        spec(this.innerText())
    }

    companion object {
        fun ComponentsAccessors.buttonByText(label: String) =
            Button(page.locator("xpath=//button[${XPath.hasText(label)}]"))

        /**
         * As this locator is not user-facing, should be used with caution. For instance,
         * when label is dynamic or not easily predictable.
         */
        fun ComponentsAccessors.buttonByTestId(testId: String) =
            Button(page.getByTestId(testId))

        /**
         * Assumes a single button in the container and returns it
         */
        fun ComponentsAccessors.buttonByContainer(container: Locator) =
            Button(container.locator("button"))
    }
}
