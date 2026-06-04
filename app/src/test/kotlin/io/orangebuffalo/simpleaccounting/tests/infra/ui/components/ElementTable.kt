package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount

class ElementTable private constructor(
    private val container: Locator,
) : UiComponent<ElementTable>() {

    private val rows = container.locator(".el-table__body tbody tr")

    fun shouldHaveRows(count: Int): ElementTable {
        rows.shouldHaveCount(count)
        return this
    }

    fun row(index: Int): Locator = rows.nth(index)

    companion object {
        fun byContainer(container: Locator) = ElementTable(container)
    }
}
