package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@UiComponentMarker
class SaOverviewFilters private constructor(
    private val container: Locator,
    private val page: Page,
) : UiComponent<SaOverviewFilters>() {
    private val editor get() = page.locator(".sa-overview-filters__editor")

    private fun chipButton(label: String): Locator {
        container.shouldSatisfy { snapshot().filter { it.label == label }.shouldHaveSize(1) }
        val button = container.getByText("$label:", Locator.GetByTextOptions().setExact(true))
            .locator("..")
        button.shouldHaveCount(1)
        return button
    }

    private fun open(label: String) {
        val button = chipButton(label)
        if (button.getAttribute("aria-expanded") != "true") {
            button.click()
            editor.shouldHaveCount(1)
        }
    }

    fun openWithKeyboard(label: String): SaOverviewFilters {
        val resultsTop = page.locator(".sa-pageable-items").boundingBox()!!.y
        chipButton(label).press("Enter")
        editor.shouldHaveCount(1)
        editor.locator("input").shouldSatisfy {
            evaluate("element => element === document.activeElement").shouldBe(true)
        }
        container.shouldSatisfy {
            page.locator(".sa-pageable-items").boundingBox()!!.y.shouldBe(resultsTop)
        }
        return this
    }

    fun closeWithEscape(label: String = "Search"): SaOverviewFilters {
        editor.getByRole(AriaRole.TEXTBOX).press("Escape")
        shouldHavePopupClosed()
        assertFocusedTrigger(label)
        return this
    }

    fun closeOptionWithEscape(label: String, value: String): SaOverviewFilters {
        editor.getByRole(AriaRole.CHECKBOX, Locator.GetByRoleOptions().setName(value).setExact(true))
            .also { it.shouldHaveCount(1) }.press("Escape")
        shouldHavePopupClosed()
        assertFocusedTrigger(label)
        return this
    }

    private fun assertFocusedTrigger(label: String) {
        chipButton(label).shouldSatisfy {
            evaluate("element => element === document.activeElement").shouldBe(true)
        }
    }

    fun applySearchWithEnter(value: String): SaOverviewFilters {
        addTextFilter(label = "Search", value = value)
        editor.getByRole(AriaRole.TEXTBOX).press("Enter")
        shouldHavePopupClosed()
        assertFocusedTrigger("Search")
        return this
    }

    fun closeByOutsideClick(): SaOverviewFilters {
        page.locator("h1").also { it.shouldHaveCount(1) }.click()
        shouldHavePopupClosed()
        return this
    }

    fun addTextFilter(label: String, value: String): SaOverviewFilters {
        open(label)
        TextInput.byContainer(editor).fill(value)
        return this
    }

    fun shouldHaveTextFilterValue(label: String, value: String): SaOverviewFilters {
        readEditor(label) { TextInput.byContainer(editor).shouldHaveValue(value) }
        return this
    }

    private fun readEditor(label: String, assertion: () -> Unit) {
        val previouslyOpen = snapshot().singleOrNull { chipButton(it.label).getAttribute("aria-expanded") == "true" }?.label
        open(label)
        try {
            assertion()
        } finally {
            if (previouslyOpen == null) closeByOutsideClick()
            else if (previouslyOpen != label) open(previouslyOpen)
        }
    }

    fun addSelectFilter(label: String, value: String): SaOverviewFilters {
        open(label)
        Checkbox.checkboxByOwnLabel(container = editor, label = value).click()
        return this
    }

    fun setDateRange(label: String, from: String, to: String): SaOverviewFilters {
        open(label)
        val inputs = editor.locator(".el-range-input")
        inputs.shouldHaveCount(2)
        inputs.nth(0).fill(from)
        inputs.nth(1).fill(to)
        inputs.nth(1).press("Enter")
        return this
    }

    fun openDateRangeCalendar(label: String): SaOverviewFilters {
        open(label)
        val inputs = editor.locator(".el-range-input")
        inputs.shouldHaveCount(2)
        inputs.nth(0).click()
        return this
    }

    fun toggleOptionWithKeyboard(label: String, value: String): SaOverviewFilters {
        open(label)
        editor.getByRole(AriaRole.CHECKBOX, Locator.GetByRoleOptions().setName(value).setExact(true)).press("Space")
        return this
    }

    fun clearWithKeyboard(label: String): SaOverviewFilters {
        removeButton(label = label, value = singleValue(label)).press("Enter")
        return this
    }

    fun shouldHaveSelectOptions(label: String, vararg values: String): SaOverviewFilters {
        readEditor(label) {
            editor.locator(".el-checkbox__label").shouldSatisfy {
                allInnerTexts().shouldContainExactly(*values)
            }
        }
        return this
    }

    fun shouldHaveChips(vararg values: OverviewFilterChipData): SaOverviewFilters {
        container.shouldSatisfy { snapshot().shouldContainExactly(*values) }
        return this
    }

    private fun snapshot(): List<OverviewFilterChipData> {
        val json = container.locator(".sa-overview-filters__chip").evaluateAll("""
            chips => JSON.stringify(chips.map(chip => ({
                label: chip.querySelector('button > span').textContent.trim().replace(/:$/, ''),
                summary: Array.from(chip.querySelectorAll('strong')).map(value => value.textContent.trim()).join(', '),
                active: chip.classList.contains('sa-overview-filters__chip--active')
            })))
        """) as String
        return Json.decodeFromString(json)
    }

    fun clear(label: String): SaOverviewFilters = removeValue(label = label, value = singleValue(label))

    private fun singleValue(label: String): String = snapshot().single { it.label == label }.summary

    private fun removeButton(label: String, value: String): Locator {
        container.shouldSatisfy { snapshot().filter { it.label == label }.shouldHaveSize(1) }
        val values = chipButton(label).locator("..").locator(".sa-overview-filters__value strong")
        values.shouldSatisfy { allTextContents().filter { it.trim() == value }.shouldHaveSize(1) }
        return container.getByRole(
            AriaRole.BUTTON,
            Locator.GetByRoleOptions().setName("Remove $label: $value").setExact(true),
        ).also { it.shouldHaveCount(1) }
    }

    fun removeValue(label: String, value: String): SaOverviewFilters {
        removeButton(label = label, value = value).click()
        return this
    }

    fun clearAll(): SaOverviewFilters {
        container.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Clear All").setExact(true)).click()
        return this
    }

    fun reportPopoverRendering(label: String, name: String): SaOverviewFilters {
        open(label)
        page.locator("body").reportRendering(name)
        return this
    }

    fun shouldHavePopupClosed(): SaOverviewFilters {
        editor.shouldHaveCount(0)
        return this
    }

    companion object {
        fun ComponentsAccessors.overviewFilters() = SaOverviewFilters(
            container = page.locator(".sa-overview-filters"),
            page = page,
        )
    }
}

@Serializable
data class OverviewFilterChipData(val label: String, val summary: String, val active: Boolean)
