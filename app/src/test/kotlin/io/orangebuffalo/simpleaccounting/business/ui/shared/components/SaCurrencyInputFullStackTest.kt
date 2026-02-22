package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.CreateExpensePage.Companion.shouldBeCreateExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import org.junit.jupiter.api.Test

/**
 * Comprehensive full stack tests for CurrencyInput component.
 * Uses Create Expense and Edit Expense pages as testing grounds.
 */
class SaCurrencyInputFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load with default currency when no value provided`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
                input.shouldHaveGroupedOptions { groups ->
                    groups.map { it.name }.shouldContainExactly("All Currencies")
                }
            }
        }
    }

    @Test
    fun `should load with provided value`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val eurExpense = expense(workspace = workspace, category = category, currency = "EUR")
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.navigate("/expenses/${preconditions.eurExpense.id}/edit")
        page.shouldBeEditExpensePage {
            currency {
                input.shouldHaveSelectedValue("EUR - Euro")
            }
        }
    }

    @Test
    fun `should display only all currencies group when shortlist is empty`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, defaultCurrency = "ADP")
                }
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                // ensure data loaded
                input.shouldHaveSelectedValue("ADP - Andorran Peseta")
                input.shouldHaveGroupedOptions { groups ->
                    groups.shouldWithClue("Should have only 'All' group when shortlist is empty") {
                        map { it.name }.shouldContainExactly("All Currencies")
                    }
                    groups[0].options.shouldWithClue("Should have many currencies (>100) in All group") {
                        size.shouldBeGreaterThan(100)
                        toSet().shouldWithClue("All currency codes should be unique") {
                            shouldHaveSize(size)
                        }
                    }

                    this@openCreateExpensePage.reportRendering("currency-input.open-empty-shortlist")
                }
            }
        }
    }

    @Test
    fun `should display recent group with single currency in shortlist`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it, defaultCurrency = "ADP")
                    val category = category(workspace = workspace, name = "Delivery")
                    expense(workspace = workspace, category = category, currency = "EUR")
                }
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveGroupedOptions { groups ->
                    groups.shouldWithClue("Should have both 'Recent' and 'All' groups") {
                        map { it.name }.shouldContainExactly("Recently Used Currencies", "All Currencies")
                    }
                    groups[0].options.shouldWithClue("Should have single EUR item in shortlist") {
                        shouldContainExactlyInAnyOrder("EUREuro")
                    }
                    groups[1].options.shouldWithClue("All group should have all currencies") {
                        shouldHaveAtLeastSize(100)
                    }

                    this@openCreateExpensePage.reportRendering("currency-input.open-single-item-shortlist")
                }
            }
        }
    }

    @Test
    fun `should display recent group with multiple currencies in shortlist`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it, defaultCurrency = "USD")
                    val category = category(workspace = workspace, name = "Delivery")
                    expense(workspace = workspace, category = category, currency = "EUR")
                    expense(workspace = workspace, category = category, currency = "GBP")
                    expense(workspace = workspace, category = category, currency = "USD")
                }
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveGroupedOptions { groups ->
                    groups.shouldWithClue("Should have both 'Recent' and 'All' groups") {
                        map { it.name }.shouldContainExactly("Recently Used Currencies", "All Currencies")
                    }
                    groups[0].options.shouldWithClue("Should have EUR, GBP, USD in shortlist") {
                        shouldContainExactlyInAnyOrder("EUREuro", "GBPBritish Pound", "USDUS Dollar")
                    }
                    groups[1].options.shouldWithClue("All group should have all currencies") {
                        shouldHaveAtLeastSize(100)
                    }

                    this@openCreateExpensePage.reportRendering("currency-input.open-multiple-items-shortlist")
                }
            }
        }
    }

    @Test
    fun `should select currency from recent group`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it, defaultCurrency = "USD")
                    val category = category(workspace = workspace, name = "Delivery")
                    expense(workspace = workspace, category = category, currency = "EUR")
                    expense(workspace = workspace, category = category, currency = "GBP")
                }
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
                // EUR appears in both "Recently Used" and "All Currencies" groups
                // Select from the first occurrence (Recently Used)
                input.selectOptionFromGroup("EUREuro", groupIndex = 0)
                input.shouldHaveSelectedValue("EUR - Euro")
                input.selectOptionFromGroup("GBPBritish Pound", groupIndex = 0)
                input.shouldHaveSelectedValue("GBP - British Pound")
            }
        }
    }

    @Test
    fun `should select currency from all currencies group`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
                input.selectOption("JPYJapanese Yen")
                input.shouldHaveSelectedValue("JPY - Japanese Yen")
                input.selectOption("AUDAustralian Dollar")
                input.shouldHaveSelectedValue("AUD - Australian Dollar")
            }
        }
    }

    @Test
    fun `should filter currencies by search text`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                // Test filtering with a search that returns exactly one result
                input.fillAndVerifyFiltered("ZMW") { filteredOptions ->
                    filteredOptions.shouldWithClue("Should have exactly one currency matching ZMW") {
                        shouldContainExactlyInAnyOrder("ZMWZambian Kwacha")
                    }
                    this@openCreateExpensePage.reportRendering("currency-input.filtered")
                }
            }
        }
    }

    @Test
    fun `should display loading state during shortlist fetch`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(preconditions.fry)

        // Block the shortlist API and verify loading state
        page.withBlockedApiResponse(
            "**/statistics/currencies-shortlist",
            initiator = {
                // Navigate to create expense page, which triggers the API call
                page.openCreateExpensePage {
                    currency {
                        input.focus()
                    }
                }
            },
            blockedRequestSpec = {
                // While API is blocked, the currency input should be visible
                // The screenshot will show it in loading state
                page.shouldBeCreateExpensePage {
                    currency {
                        input.shouldBeLoading()
                    }
                    reportRendering("currency-input.loading")
                }
            }
        )
    }
}
