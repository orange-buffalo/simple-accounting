package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue

/**
 * Component wrapper for SaEntitySelect.vue - a remote select component that
 * fetches entities dynamically via remote search with pagination support.
 */
class EntitySelect private constructor(
    private val rootLocator: Locator,
) : UiComponent<EntitySelect>() {
    private val input = rootLocator.locator(".el-select__wrapper")

    fun shouldBeVisible() = input.shouldBeVisible()

    fun shouldBeHidden() = input.shouldBeHidden()

    /**
     * Types a search query into the remote select field.
     * This triggers the remote search functionality.
     */
    fun search(query: String) {
        // Open the dropdown if not already open
        input.click()
        // Type into the filterable input
        val searchInput = input.locator("input.el-select__input")
        searchInput.fill(query)
    }

    /**
     * Selects an option from the dropdown by its text.
     * Works with custom option templates - will click on any option containing the text.
     */
    fun selectOption(optionText: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        // For entity select, options can have complex structure, so we search for text anywhere in the option
        popper.rootLocator
            .locator(".el-select-dropdown__item")
            .filter(Locator.FilterOptions().setHasText(optionText))
            .first()
            .click()
        popper.shouldBeClosed()
        shouldHaveSelectedValue(optionText)
    }

    /**
     * Searches for an entity and selects it from the results.
     * Combines search and selection in one operation.
     */
    fun searchAndSelect(query: String, optionText: String) {
        search(query)
        selectOption(optionText)
    }

    fun shouldHaveSelectedValue(value: String) {
        // EntitySelect stores the label as the selected value after initial load
        input.locator(".el-select__selected-item span:not([aria-hidden])")
            .shouldHaveText(value)
    }

    /**
     * Verifies that options are displayed in the dropdown.
     * Opens dropdown if needed.
     * @param options Expected option texts (can be partial text matches due to custom templates)
     */
    fun shouldHaveOptions(vararg options: String) {
        shouldHaveOptions { actualOptions ->
            actualOptions.shouldWithClue("Expected options: ${options.toList()}") {
                shouldContainExactly(*options)
            }
        }
    }

    fun shouldHaveOptions(spec: (actualOptions: List<String>) -> Unit) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator.shouldSatisfy {
            // Get all non-info items (exclude pagination/error messages)
            locator(".el-select-dropdown__item:not([disabled])")
                .allInnerTexts()
                .should(spec)
        }
    }

    /**
     * Verifies that the "more elements" pagination indicator is shown.
     * This appears when there are more results than the page size.
     */
    fun shouldShowMoreElementsIndicator(remainingCount: Int) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator
            .locator(".el-select-dropdown__item.is-disabled .sa-entity-select__list-footer--dimmed")
            .shouldBeVisible()
            .shouldHaveText("$remainingCount more elements...")
    }

    /**
     * Verifies that the "more elements" indicator is not shown.
     */
    fun shouldNotShowMoreElementsIndicator() {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator
            .locator(".el-select-dropdown__item.is-disabled .sa-entity-select__list-footer--dimmed")
            .shouldBeHidden()
    }

    /**
     * Verifies that the loading state is displayed.
     */
    fun shouldBeLoading() {
        rootLocator.locator(".sa-input-loader__indicator").shouldBeVisible()
    }

    /**
     * Verifies that the component is not in loading state.
     */
    fun shouldNotBeLoading() {
        rootLocator.locator(".sa-input-loader__indicator").shouldBeHidden()
    }

    /**
     * Verifies that an error state is displayed in the dropdown.
     */
    fun shouldShowError() {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator
            .locator(".el-select-dropdown__item.is-disabled .sa-basic-error-message")
            .shouldBeVisible()
    }

    /**
     * Clears the current selection by clicking the clear button.
     * Only works when clearable=true is set on the component.
     */
    fun clearSelection() {
        // Hover to make clear button visible
        input.hover()
        val clearIcon = input.locator(".el-select__clear")
        clearIcon.click()
    }

    /**
     * Verifies that the clear button is visible.
     */
    fun shouldHaveClearButton() {
        input.hover()
        input.locator(".el-select__clear").shouldBeVisible()
    }

    /**
     * Verifies that the select is in an empty/unselected state.
     */
    fun shouldBeEmpty() {
        input.locator(".el-select__placeholder").shouldBeVisible()
    }

    /**
     * Verifies that the select shows the specified placeholder text.
     */
    fun shouldHavePlaceholder(placeholder: String) {
        input.locator(".el-select__placeholder").shouldHaveText(placeholder)
    }

    /**
     * Opens the dropdown to trigger the initial remote search.
     */
    fun openDropdown() {
        input.click()
    }

    companion object {
        fun byContainer(container: Locator) = EntitySelect(container.locator(".sa-entity-select"))
    }
}
