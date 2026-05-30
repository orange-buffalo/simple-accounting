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

    fun shouldHaveActiveValues(vararg values: String): SaOverviewFilters {
        filterButton.shouldHaveText("Add filters")
        activeValues.shouldSatisfy {
            allInnerTexts().shouldContainExactly(*values)
        }
        return this
    }

    fun shouldHaveNoActiveValues(): SaOverviewFilters {
        filterButton.shouldHaveText("Add filters")
        activeValues.shouldSatisfy {
            allInnerTexts().shouldContainExactly()
        }
        return this
    }

    fun addSelectFilter(label: String, value: String): SaOverviewFilters {
        openPopover()
        selectFilterOption(label, value)
        apply()
        return this
    }

    fun addSelectFilterAndCancel(label: String, value: String): SaOverviewFilters {
        openPopover()
        selectFilterOption(label, value)
        cancel()
        return this
    }

    fun clearAll(): SaOverviewFilters {
        openPopover()
        popover.locator("button", Locator.LocatorOptions().setHasText("Clear All")).click()
        apply()
        return this
    }

    fun removeActiveValue(value: String): SaOverviewFilters {
        activeValues.filter(Locator.FilterOptions().setHasText(value))
            .locator(".el-tag__close")
            .click()
        return this
    }

    fun addTextFilter(label: String, value: String): SaOverviewFilters {
        openPopover()
        TextInput.byContainer(filterControl(label)).fill(value)
        apply()
        return this
    }

    fun reportPopoverRendering(name: String): SaOverviewFilters {
        openPopover()
        popover.reportRendering(name)
        cancel()
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

    private fun apply() {
        popover.locator("button", Locator.LocatorOptions().setHasText("Apply")).click()
        shouldHavePopupClosed()
    }

    private fun cancel() {
        popover.locator("button", Locator.LocatorOptions().setHasText("Cancel")).click()
        shouldHavePopupClosed()
    }

    private fun openPopover() {
        if (popover.isVisible) {
            return
        }
        filterButton.click()
        popover.shouldBeVisible()
    }

    private fun filterControl(label: String) = popover.locator(".sa-overview-page__filter-control:has-text('$label')")

    private fun selectFilterOption(label: String, value: String) {
        val control = filterControl(label)
        control.locator(".el-select__wrapper").click()
        control.locator(".el-select-dropdown__item", Locator.LocatorOptions().setHasText(value)).click()
        control.locator(".el-select__wrapper").click()
    }

    companion object {
        fun ComponentsAccessors.overviewFilters() = SaOverviewFilters(page)
    }
}
