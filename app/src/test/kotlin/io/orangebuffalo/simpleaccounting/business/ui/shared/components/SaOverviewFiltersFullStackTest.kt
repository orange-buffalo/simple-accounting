package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.categories.CategoriesOverviewPage.Companion.openCategoriesOverviewPage
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
            filters.shouldHaveNoActiveValues()
            filters.reportPopoverRendering("overview-filters.popover")

            filters.addFilter("Type", "Income")
            filters.shouldHaveActiveValues("Type: Income")
            pageItems.shouldHaveTitles("Slurm supplies", "Delivery")
            reportRendering("overview-filters.active-value")

            filters.addFilterAndCancel("Type", "Expense")
            filters.shouldHaveActiveValues("Type: Income")
            pageItems.shouldHaveTitles("Slurm supplies", "Delivery")

            filters.addFilter("Type", "Expense")
            filters.shouldHaveActiveValues("Type: Income", "Type: Expense")
            pageItems.shouldHaveTitles("Slurm supplies", "Robot maintenance", "Delivery")

            filters.removeActiveValue("Type: Income")
            filters.shouldHaveActiveValues("Type: Expense")
            pageItems.shouldHaveTitles("Robot maintenance", "Delivery")

            filters.clearAll()
            filters.shouldHaveNoActiveValues()
            pageItems.shouldHaveTitles("Slurm supplies", "Robot maintenance", "Delivery")
        }
    }
}
