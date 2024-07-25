package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.string.shouldNotBeBlank
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldNotBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

/**
 * Internal for components, to not be used directly in tests.
 */
class Popper(
    page: Page,
    id: String,
) {
    val rootLocator : Locator = page.locator("#$id")

    fun shouldBeClosed() {
        rootLocator.shouldNotBeVisible()
    }

    companion object {
        fun openOrLocateByTrigger(trigger: Locator): Popper {
            var popperId = trigger.getAttribute(POPPER_REF_ATTRIBUTE)
            // if not yet open, trigger and get ID
            if (popperId == null) {
                trigger.click()
                trigger.shouldSatisfy {
                    popperId = trigger.getAttribute(POPPER_REF_ATTRIBUTE).shouldNotBeBlank()
                }
            }
            return Popper(trigger.page(), popperId)
        }
    }
}

private const val POPPER_REF_ATTRIBUTE = "aria-describedby"
