package io.orangebuffalo.simpleaccounting.business.ui.user.categories

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Select
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

class CategoriesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Categories")
    val pageItems = components.overviewItems()
    val createButton = components.buttonByText("Add new")
    private val filtersButton = components.buttonByText("Filters")
    private val filtersPopover = page.locator(".sa-overview-page__filters-popover")
    private val activeFilters = page.locator(".sa-overview-page__active-filters")

    fun openFilters(): CategoriesOverviewPage {
        filtersButton.click()
        filtersPopover.shouldBeVisible()
        return this
    }

    fun selectTypeFilter(option: String): CategoriesOverviewPage {
        Select.byContainer(filtersPopover.locator(".sa-overview-page__filter-control:has-text('Type')"))
            .selectOption(option)
        return this
    }

    fun shouldHaveActiveFilter(value: String): CategoriesOverviewPage {
        activeFilters.locator(".el-tag").shouldHaveText(value)
        return this
    }

    fun reportFiltersPopoverRendering(name: String): CategoriesOverviewPage {
        filtersPopover.reportRendering(name)
        return this
    }

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCategoriesOverviewPage(spec: CategoriesOverviewPage.() -> Unit = {}) {
            CategoriesOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCategoriesOverviewPage(spec: CategoriesOverviewPage.() -> Unit = {}) {
            navigate("/settings/categories")
            shouldBeCategoriesOverviewPage(spec)
        }
    }
}
