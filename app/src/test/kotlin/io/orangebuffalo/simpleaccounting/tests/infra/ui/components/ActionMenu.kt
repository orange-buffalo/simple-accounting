package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class ActionMenu private constructor(
    private val page: Page,
    private val trigger: Locator,
) : UiComponent<ActionMenu>() {

    fun open() {
        trigger.click()
    }

    fun shouldHaveItems(vararg labels: String) {
        open()
        page.locator(".sa-action-menu__items .el-button").allInnerTexts()
            .map { it.trim() }
            .shouldContainExactlyInAnyOrder(*labels)
    }

    fun clickItem(label: String) {
        open()
        page.locator(".sa-action-menu__items .el-button", Page.LocatorOptions().setHasText(label)).click()
    }

    companion object {
        fun byContainer(container: Locator) = ActionMenu(
            page = container.page(),
            trigger = container.locator(".sa-action-menu__trigger"),
        )
    }
}
