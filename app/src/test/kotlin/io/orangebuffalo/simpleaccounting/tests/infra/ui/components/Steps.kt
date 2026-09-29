package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
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

    fun navigateToPreviousStep(title: String) {
        container.locator(".el-step__title").getByRole(
            AriaRole.BUTTON,
            Locator.GetByRoleOptions().setName(title).setExact(true)
        ).click()
    }

    companion object {
        fun ComponentsAccessors.stepsByContainer(container: Locator) =
            Steps(container)
    }
}
