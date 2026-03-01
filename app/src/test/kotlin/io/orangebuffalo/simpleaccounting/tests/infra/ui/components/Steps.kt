package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

class Steps private constructor(
    private val container: Locator,
) : UiComponent<Steps>() {

    fun shouldHaveStepDescriptions(vararg descriptions: String) {
        shouldSatisfy("Steps should have expected descriptions") {
            container.locator(".el-step .el-step__description")
                .allInnerTexts()
                .shouldContainExactly(*descriptions)
        }
    }

    companion object {
        fun ComponentsAccessors.stepsByContainer(container: Locator) =
            Steps(container)
    }
}
