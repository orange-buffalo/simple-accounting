package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.categories.CategoriesOverviewPage.Companion.openCategoriesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.OverviewFilterChipData
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import org.junit.jupiter.api.Test

class SaOverviewFiltersFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should manage active filter values`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    category(workspace = workspace, name = "Slurm supplies", income = true, expense = false)
                    category(workspace = workspace, name = "Robot maintenance", income = false, expense = true)
                    category(workspace = workspace, name = "Delivery", income = true, expense = true)
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openCategoriesOverviewPage {
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Any", active = false),
                OverviewFilterChipData(label = "Type", summary = "Any", active = false),
            )
            filters.openWithKeyboard("Search")
            filters.addTextFilter(label = "Search", value = "Slurm")
            pageItems.shouldHaveTitles("Slurm supplies")
            filters.closeWithEscape()
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Slurm", active = true),
                OverviewFilterChipData(label = "Type", summary = "Any", active = false),
            )
            filters.clearWithKeyboard("Search")
            pageItems.shouldHaveTitles("Slurm supplies", "Robot maintenance", "Delivery")
            filters.applySearchWithEnter("Robot")
            pageItems.shouldHaveTitles("Robot maintenance")
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Robot", active = true),
                OverviewFilterChipData(label = "Type", summary = "Any", active = false),
            )
            filters.addTextFilter(label = "Search", value = "Robot")
            filters.closeByOutsideClick()
            filters.clear("Search")
            pageItems.shouldHaveTitles("Slurm supplies", "Robot maintenance", "Delivery")

            filters.shouldHaveSelectOptions(label = "Type", "Income", "Expense")
            filters.reportPopoverRendering(label = "Type", name = "overview-filters.popover")
            filters.closeOptionWithEscape(label = "Type", value = "Income")
            filters.addSelectFilter(label = "Type", value = "Income")
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Any", active = false),
                OverviewFilterChipData(label = "Type", summary = "Income", active = true),
            )
            pageItems.shouldHaveTitles("Slurm supplies", "Delivery")
            reportRendering("overview-filters.active-value")

            filters.toggleOptionWithKeyboard(label = "Type", value = "Expense")
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Any", active = false),
                OverviewFilterChipData(label = "Type", summary = "Income, Expense", active = true),
            )
            pageItems.shouldHaveTitles("Slurm supplies", "Robot maintenance", "Delivery")

            filters.addTextFilter(label = "Search", value = "Delivery")
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Delivery", active = true),
                OverviewFilterChipData(label = "Type", summary = "Income, Expense", active = true),
            )
            pageItems.shouldHaveTitles("Delivery")
            filters.removeValue(label = "Type", value = "Income")
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Delivery", active = true),
                OverviewFilterChipData(label = "Type", summary = "Expense", active = true),
            )
            pageItems.shouldHaveTitles("Delivery")
            filters.addSelectFilter(label = "Type", value = "Expense")
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Delivery", active = true),
                OverviewFilterChipData(label = "Type", summary = "Any", active = false),
            )
            pageItems.shouldHaveTitles("Delivery")

            filters.addSelectFilter(label = "Type", value = "Income")
            filters.clearAll()
            filters.shouldHaveChips(
                OverviewFilterChipData(label = "Search", summary = "Any", active = false),
                OverviewFilterChipData(label = "Type", summary = "Any", active = false),
            )
            pageItems.shouldHaveTitles("Slurm supplies", "Robot maintenance", "Delivery")
        }
    }
}
