package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

class CurrencyInput private constructor(
    private val rootLocator: Locator,
) : UiComponent<CurrencyInput>() {
    private val select = Select.byContainer(rootLocator)
    private val input = rootLocator.locator(".el-select__wrapper")

    /**
     * Selects a currency by matching its innerText (e.g., "EUREuro").
     * Works with the custom currency option template that displays code and name separately.
     * The innerText format is "CODEName" with no separator (e.g., "EUREuro", "USDUS Dollar").
     */
    fun selectOption(currencyInnerText: String) {
        val popper = Popper.openOrLocateByTrigger(input)
        // Currency options have custom markup with separate spans for code and name
        // The innerText concatenates them without separator
        popper.rootLocator
            .locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}][normalize-space(.)='$currencyInnerText']")
            .click()
        popper.shouldBeClosed()
    }

    /**
     * Selects a currency from a specific group by index.
     * Use this when the same currency appears in multiple groups (e.g., "Recently Used" and "All Currencies").
     * 
     * @param currencyInnerText The currency's innerText (e.g., "EUREuro")
     * @param groupIndex The zero-based index of the group (0 for first group, 1 for second)
     */
    fun selectOptionFromGroup(currencyInnerText: String, groupIndex: Int) {
        val popper = Popper.openOrLocateByTrigger(input)
        // Locate the specific group, then find the option within it
        val groups = popper.rootLocator.locator("xpath=//*[${XPath.hasClass("el-select-group__wrap")}]")
        val targetGroup = groups.nth(groupIndex)
        // Use .first() here because we're already scoped to a specific group
        targetGroup
            .locator("xpath=.//*[${XPath.hasClass("el-select-dropdown__item")}][normalize-space(.)='$currencyInnerText']")
            .click()
        popper.shouldBeClosed()
    }

    /**
     * Fills the filter input and verifies the filtered options.
     * This method handles the interaction properly by filling the input,
     * waiting for the filter to apply, and then checking the filtered results.
     * 
     * @param filterText The text to type into the filter
     * @param verifyAndAction Lambda that receives the filtered options and can verify them + take screenshots
     */
    fun fillAndVerifyFiltered(filterText: String, verifyAndAction: (List<String>) -> Unit) {
        // Click to open the dropdown
        input.click()
        
        // Fill the filter text in the actual input element
        val actualInput = input.locator("input.el-select__input")
        actualInput.fill(filterText)
        
        // Get the filtered options from the visible dropdown
        // Only get visible options (non-filtered ones have display:none)
        // Use shouldSatisfy to retry until filtering is applied
        val popper = Popper.openOrLocateByTrigger(input)
        val visibleOptions = mutableListOf<String>()
        popper.rootLocator.shouldSatisfy("Filtered options should be available") {
            @Suppress("UNCHECKED_CAST")
            val options = popper.rootLocator
                .locator("xpath=//*[${XPath.hasClass("el-select-dropdown__item")}]")
                .evaluateAll("elements => elements.filter(el => el.offsetParent !== null).map(el => el.textContent.trim())") as List<String>
            // Store options for verification
            visibleOptions.clear()
            visibleOptions.addAll(options)
            // Assert that we have some filtered results (retry will continue until filter is applied)
            options.isNotEmpty()
        }
        
        // Call the verification lambda
        verifyAndAction(visibleOptions)
        
        // Close the dropdown by pressing Escape
        actualInput.press("Escape")
    }

    fun shouldHaveSelectedValue(value: String) = select.shouldHaveSelectedValue(value)
    
    /**
     * Checks if the select component is in loading state.
     * The loading state is indicated by the loading icon within the select wrapper.
     */
    fun shouldBeLoading() {
        // ElSelect shows a loading icon when the :loading prop is true
        // It uses the el-icon--loading class within the select wrapper
        input.locator(".el-icon--loading").shouldBeVisible()
    }
    
    fun fill(text: String) {
        input.click()
        // Find the actual input element inside the wrapper for filtering
        val actualInput = input.locator("input.el-select__input")
        actualInput.fill(text)
    }

    fun shouldBeVisible() = select.shouldBeVisible()

    fun shouldBeHidden() = select.shouldBeHidden()

    fun shouldBeDisabled() = select.shouldBeDisabled()

    fun shouldHaveGroupedOptions(spec: (actualOptions: List<OptionsGroup>) -> Unit) =
        select.shouldHaveGroupedOptions(spec)

    companion object {
        fun byContainer(container: Locator) = CurrencyInput(container)
    }
}
