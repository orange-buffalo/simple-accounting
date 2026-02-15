package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.ui.user.InvoiceOption

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
        input.click()
        val searchInput = rootLocator.locator("input.el-select__input")
        searchInput.fill(query)
    }

    /**
     * Selects an option from the dropdown by its text.
     * For remote selects, searches for the option text first.
     */
    fun selectOption(optionText: String) {
        rootLocator.shouldBeVisible()
        search(optionText)
        
        val popper = Popper.openOrLocateByTrigger(input)
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
        input.locator(".el-select__selected-item span:not([aria-hidden])")
            .shouldHaveText(value)
    }

    /**
     * Verifies that options are displayed in the dropdown.
     * Opens dropdown if needed.
     */
    fun shouldHaveOptions(vararg options: String) {
        shouldHaveOptions { actualOptions ->
            actualOptions.shouldWithClue("Expected options: ${options.toList()}") {
                shouldContainExactly(*options)
            }
        }
    }

    fun shouldHaveOptions(spec: (actualOptions: List<String>) -> Unit) {
        withDropdownOpen {
            shouldSatisfy {
                locator(".el-select-dropdown__item:not([disabled])")
                    .allInnerTexts()
                    .should(spec)
            }
        }
    }

    /**
     * Verifies invoice options with rich content (title, date, amount).
     * Parses the invoice option structure and validates against expected data.
     */
    fun shouldHaveInvoiceOptions(spec: (actualOptions: List<InvoiceOption>) -> Unit) {
        withDropdownOpen {
            shouldSatisfy {
                // language=javascript
                val actualOptionsJson = evaluate(
                    """
                    (popper) => JSON.stringify(Array.from(popper
                        .querySelectorAll('.el-select-dropdown__item:not([disabled])'))
                        .map(item => {
                            const option = item.querySelector('.sa-invoice-select__option');
                            if (!option) return null;
                            return {
                                title: option.querySelector('.sa-invoice-select__option__title')?.innerText || '',
                                date: option.querySelector('.sa-invoice-select__option__date')?.innerText || '',
                                amount: option.querySelector('.sa-invoice-select__option__amount')?.innerText || ''
                            };
                        })
                        .filter(opt => opt !== null))
                    """
                ) as String
                val actualOptions = Gson().fromJson(actualOptionsJson, object : TypeToken<List<InvoiceOption>>() {})
                spec(actualOptions)
            }
        }
    }

    /**
     * Verifies that the "more elements" pagination indicator is shown.
     * This appears when there are more results than the page size.
     */
    fun shouldShowMoreElementsIndicator(remainingCount: Int) {
        withDropdownOpen {
            val indicator = locator(".el-select-dropdown__item.is-disabled .sa-entity-select__list-footer--dimmed")
            indicator.shouldBeVisible()
            indicator.shouldHaveText("$remainingCount more items..")
        }
    }

    /**
     * Verifies that the "more elements" indicator is not shown.
     */
    fun shouldNotShowMoreElementsIndicator() {
        withDropdownOpen {
            locator(".el-select-dropdown__item.is-disabled .sa-entity-select__list-footer--dimmed")
                .shouldBeHidden()
        }
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
        withDropdownOpen {
            locator(".el-select-dropdown__item.is-disabled .sa-basic-error-message")
                .shouldBeVisible()
        }
    }

    /**
     * Clears the current selection by clicking the clear button.
     * Only works when clearable=true is set on the component.
     */
    fun clearSelection() {
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
     * Executes the given specification with the dropdown open.
     * Opens the dropdown if not already open.
     *
     * @param spec The specification to execute while the dropdown is open. The spec receives the popper's root locator.
     */
    private fun withDropdownOpen(spec: Locator.() -> Unit) {
        val popper = Popper.openOrLocateByTrigger(input)
        popper.rootLocator.shouldBeVisible()
        popper.rootLocator.spec()
    }

    companion object {
        fun byContainer(container: Locator) = EntitySelect(container)
    }
}
