package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldNotBeBlank
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

@UiComponentMarker
class Tooltip private constructor(private val trigger: Locator) : UiComponent<Tooltip>() {
    fun shouldHaveText(expectedText: String) {
        trigger.hover()
        val popperId = trigger.getAttribute("aria-describedby").shouldNotBeBlank().shouldNotBeNull()
        Popper(trigger.page(), popperId).rootLocator.shouldHaveText(expectedText)
    }

    companion object {
        fun byTrigger(trigger: Locator) = Tooltip(trigger)
    }
}
