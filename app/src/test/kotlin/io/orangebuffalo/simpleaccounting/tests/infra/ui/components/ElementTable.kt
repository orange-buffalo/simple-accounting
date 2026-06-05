package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.tests.infra.utils.injectJsUtils
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ElementTable private constructor(
    private val container: Locator,
) : UiComponent<ElementTable>() {

    private val rows = container.locator(".el-table__body tbody tr")

    fun shouldHaveRows(vararg expectedRows: ElementTableRowData): ElementTable {
        container.shouldSatisfy("Element Plus table rows should match expected content") {
            rowsData().shouldContainExactly(*expectedRows)
        }
        return this
    }

    fun row(index: Int): Locator = rows.nth(index)

    private fun rowsData(): List<ElementTableRowData> {
        val rowsJson = container.evaluate(
            """
            (container) => {
                ${injectJsUtils()}
                const rows = Array.from(container.querySelectorAll('.el-table__body tbody tr'))
                    .filter(row => row.offsetParent !== null);
                return JSON.stringify(rows.map(row => ({
                    cells: Array.from(row.querySelectorAll('.el-table__cell')).map(cell => {
                        return utils.getDynamicContent(cell);
                    })
                })));
            }
            """,
        ) as String

        return Json.decodeFromString(rowsJson)
    }

    companion object {
        fun byContainer(container: Locator) = ElementTable(container)
    }
}

@Serializable
data class ElementTableRowData(
    val cells: List<String?>,
)
