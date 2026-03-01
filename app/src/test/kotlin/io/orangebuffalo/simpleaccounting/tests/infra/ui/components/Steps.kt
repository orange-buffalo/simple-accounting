package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldContainText

class Steps private constructor(
    private val container: Locator,
) : UiComponent<Steps>() {

    fun shouldHaveStepDescription(stepIndex: Int, description: String) {
        container.locator(".el-step").nth(stepIndex)
            .locator(".el-step__description").shouldContainText(description)
    }

    companion object {
        fun ComponentsAccessors.stepsByContainer(container: Locator) =
            Steps(container)
    }
}
