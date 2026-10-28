package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldNotBeBlank
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

@UiComponentMarker
class Tooltip private constructor(private val trigger: Locator) : UiComponent<Tooltip>() {
    fun shouldHaveText(expectedText: String) {
        trigger.hover()
        shouldHaveVisibleText(expectedText)
    }

    fun shouldHaveTextOnFocus(expectedText: String) {
        trigger.focus()
        shouldHaveVisibleText(expectedText)
    }

    private fun shouldHaveVisibleText(expectedText: String) {
        trigger.shouldSatisfy { getAttribute("aria-describedby").shouldNotBeNull().shouldNotBeBlank() }
        val popperId = trigger.getAttribute("aria-describedby").shouldNotBeBlank().shouldNotBeNull()
        Popper(page = trigger.page(), id = popperId).rootLocator.apply {
            shouldBeVisible()
            shouldHaveText(expectedText)
        }
    }

    companion object {
        fun byTrigger(trigger: Locator) = Tooltip(trigger)
    }
}
