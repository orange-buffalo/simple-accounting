package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldNotBeBlank
import io.orangebuffalo.kotestplaywrightassertions.shouldBeDisabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeEnabled
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

class ActionMenu private constructor(
    private val page: Page,
    private val trigger: Locator,
) : UiComponent<ActionMenu>() {

    fun open() {
        page.keyboard().press("Escape")
        trigger.click()
        popover().shouldBeVisible()
    }

    fun shouldHaveItems(vararg labels: String) {
        open()
        itemButtons().allInnerTexts()
            .map { it.trim() }
            .shouldContainExactly(*labels)
    }

    fun clickItem(label: String) {
        open()
        itemButton(label).click()
    }

    fun shouldHaveItemDisabled(label: String) {
        open()
        itemButton(label).shouldBeDisabled()
    }

    fun shouldHaveItemEnabled(label: String) {
        open()
        itemButton(label).shouldBeEnabled()
    }

    fun shouldHaveItemTooltip(label: String, tooltip: String) {
        open()
        val itemTrigger = itemButton(label)
            .locator("xpath=ancestor::*[contains(concat(' ', normalize-space(@class), ' '), ' el-tooltip__trigger ')][1]")
        itemTrigger.hover()
        itemTrigger.shouldSatisfy {
            val popperId = getAttribute("aria-describedby").shouldNotBeBlank()
            page.locator("#$popperId").shouldHaveText(tooltip)
        }
    }

    fun shouldBeDisabled() {
        trigger.shouldBeDisabled()
    }

    fun shouldHaveTooltip(tooltip: String) {
        trigger.hover()
        page.locator(".el-popper")
            .filter(Locator.FilterOptions().setHasText(tooltip))
            // Previous action tooltips with identical text may still be mounted; Element Plus appends the active one last.
            .last()
            .shouldBeVisible()
    }

    private fun popover() = page.locator(".sa-action-menu__popover:visible").last()

    private fun itemButtons() = popover().locator(".sa-action-menu__items .el-button")

    private fun itemButton(label: String) = itemButtons()
        .filter(Locator.FilterOptions().setHasText(label))

    companion object {
        fun byContainer(container: Locator) = ActionMenu(
            page = container.page(),
            trigger = container.locator(".sa-action-menu__trigger"),
        )
    }
}
