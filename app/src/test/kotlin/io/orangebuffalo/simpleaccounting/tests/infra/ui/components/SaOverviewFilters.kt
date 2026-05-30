package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

@UiComponentMarker
class SaOverviewFilters private constructor(
    private val page: Page,
) : UiComponent<SaOverviewFilters>() {
    private val filterButton = page.locator("button.sa-overview-page__filters-button")
    private val popover = page.locator(".sa-overview-page__filters-popover")
    private val activeValues = page.locator(".sa-overview-page__active-filters .el-tag")
    private val searchInput = TextInput.byContainer(page.locator(".sa-overview-page__filter-input"))

    fun shouldHaveActiveValues(vararg values: String): SaOverviewFilters {
        filterButton.shouldHaveText("Filters (${values.size})")
        activeValues.shouldSatisfy {
            allInnerTexts().shouldContainExactly(*values)
        }
        return this
    }

    fun shouldHaveNoActiveValues(): SaOverviewFilters {
        filterButton.shouldHaveText("Filters")
        activeValues.shouldSatisfy {
            allInnerTexts().shouldContainExactly()
        }
        return this
    }

    fun addFilter(label: String, value: String): SaOverviewFilters {
        openPopover()
        Select.byContainer(filterControl(label)).selectOption(value, validate = false)
        closePopover()
        return this
    }

    fun clearAll(): SaOverviewFilters {
        openPopover()
        popover.locator("button", Locator.LocatorOptions().setHasText("Clear All")).click()
        shouldHavePopupClosed()
        return this
    }

    fun removeActiveValue(value: String): SaOverviewFilters {
        activeValues.filter(Locator.FilterOptions().setHasText(value))
            .locator(".el-tag__close")
            .click()
        return this
    }

    fun setFreeSearchText(value: String): SaOverviewFilters {
        searchInput.fill(value)
        return this
    }

    fun reportPopoverRendering(name: String): SaOverviewFilters {
        openPopover()
        popover.reportRendering(name)
        closePopover()
        return this
    }

    fun shouldHavePopupClosed(): SaOverviewFilters {
        popover.shouldBeHidden()
        return this
    }

    private fun closePopover(): SaOverviewFilters {
        if (popover.isVisible) {
            filterButton.click()
        }
        shouldHavePopupClosed()
        return this
    }

    private fun openPopover() {
        if (popover.isVisible) {
            return
        }
        filterButton.click()
        popover.shouldBeVisible()
    }

    private fun filterControl(label: String) = popover.locator(".sa-overview-page__filter-control:has-text('$label')")

    companion object {
        fun ComponentsAccessors.overviewFilters() = SaOverviewFilters(page)
    }
}
