package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditExpensePage.Companion.shouldBeEditExpensePage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Comprehensive full stack tests for CurrencyInput component.
 * Uses Create Expense and Edit Expense pages as testing grounds.
 */
class CurrencyInputFullStackTest : SaFullStackTestBase() {

    @BeforeEach
    fun setup(page: Page) {
        // Resume clock to allow component to load shortlist asynchronously
        page.clock().resume()
    }

    @Test
    fun `should load with default currency when no value provided`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
                // Open dropdown to show both input and popover
                input.shouldHaveGroupedOptions { groups ->
                    groups.shouldHaveSize(1)
                    groups[0].name.shouldBe("All Currencies")
                }
            }

            reportRendering("currency-input.default-value-empty-shortlist")
        }
    }

    @Test
    fun `should load with provided value`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
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
                // Open dropdown
                input.shouldHaveGroupedOptions { }
            }

            reportRendering("currency-input.pre-loaded-value")
        }
    }

    @Test
    fun `should display only all currencies group when shortlist is empty`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveGroupedOptions { groups ->
                    groups.shouldWithClue("Should have only 'All' group when shortlist is empty") {
                        shouldHaveSize(1)
                    }
                    groups[0].name.shouldBe("All Currencies")
                    groups[0].options.shouldWithClue("Should verify key currencies are present") {
                        // Verify exact match for key currencies
                        filter { it == "USD - US Dollar" }.shouldHaveSize(1)
                        filter { it == "EUR - Euro" }.shouldHaveSize(1)
                        filter { it == "GBP - British Pound" }.shouldHaveSize(1)
                        filter { it == "JPY - Japanese Yen" }.shouldHaveSize(1)
                    }
                }
            }

            reportRendering("currency-input.open-empty-shortlist")
        }
    }

    @Test
    fun `should display recent group with single currency in shortlist`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val eurExpense = expense(workspace = workspace, category = category, currency = "EUR")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveGroupedOptions { groups ->
                    groups.shouldWithClue("Should have both 'Recent' and 'All' groups") {
                        shouldHaveSize(2)
                    }
                    groups[0].name.shouldBe("Recently Used Currencies")
                    groups[0].options.shouldWithClue("Should have single item in shortlist") {
                        shouldContainExactlyInAnyOrder("EUR - Euro")
                    }
                    groups[1].name.shouldBe("All Currencies")
                    groups[1].options.shouldWithClue("Shortlisted currency should not appear in All list") {
                        shouldNotContain("EUR - Euro")
                    }
                }
            }

            reportRendering("currency-input.open-single-item-shortlist")
        }
    }

    @Test
    fun `should display recent group with multiple currencies in shortlist`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val eurExpense = expense(workspace = workspace, category = category, currency = "EUR")
                val gbpExpense = expense(workspace = workspace, category = category, currency = "GBP")
                val usdExpense = expense(workspace = workspace, category = category, currency = "USD")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveGroupedOptions { groups ->
                    groups.shouldWithClue("Should have both 'Recent' and 'All' groups") {
                        shouldHaveSize(2)
                    }
                    groups[0].name.shouldBe("Recently Used Currencies")
                    groups[0].options.shouldWithClue("Should have all shortlisted currencies") {
                        shouldContainExactlyInAnyOrder("EUR - Euro", "GBP - British Pound", "USD - US Dollar")
                    }
                    groups[1].name.shouldBe("All Currencies")
                    groups[1].options.shouldWithClue("Shortlisted currencies should not appear in All list") {
                        shouldNotContain("EUR - Euro")
                        shouldNotContain("GBP - British Pound") 
                        shouldNotContain("USD - US Dollar")
                    }
                }
            }

            reportRendering("currency-input.open-multiple-items-shortlist")
        }
    }

    @Test
    fun `should select currency from recent group`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val eurExpense = expense(workspace = workspace, category = category, currency = "EUR")
                val gbpExpense = expense(workspace = workspace, category = category, currency = "GBP")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
                input.selectOption("EUR - Euro")
                input.shouldHaveSelectedValue("EUR - Euro")
                input.selectOption("GBP - British Pound")
                input.shouldHaveSelectedValue("GBP - British Pound")
            }
        }
    }

    @Test
    fun `should select currency from all currencies group`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                input.shouldHaveSelectedValue("USD - US Dollar")
                input.selectOption("JPY - Japanese Yen")
                input.shouldHaveSelectedValue("JPY - Japanese Yen")
                input.selectOption("AUD - Australian Dollar")
                input.shouldHaveSelectedValue("AUD - Australian Dollar")
            }
        }
    }

    @Test
    fun `should filter currencies by search text`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val eurExpense = expense(workspace = workspace, category = category, currency = "EUR")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                // Fill filter text
                input.fill("Doll")
                
                // Verify filtered options
                input.shouldHaveGroupedOptions { groups ->
                    // Should show filtered results - currencies containing "Doll"
                    groups.forEach { group ->
                        group.options.forEach { option ->
                            option.shouldWithClue("All filtered options should contain 'Doll'") {
                                contains("Doll", ignoreCase = true).shouldBe(true)
                            }
                        }
                    }
                }
            }

            reportRendering("currency-input.filtered")
        }
    }

    @Test
    fun `should display loading state during shortlist fetch`(page: Page) {
        val preconditions = preconditions {
            object {
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        
        page.withBlockedApiResponse(
            "**/statistics/currencies-shortlist",
            initiator = {
                page.navigate("/expenses/create")
            },
            blockedRequestSpec = {
                page.openCreateExpensePage {
                    currency {
                        // Component should show loading indicator
                        input.shouldBeVisible()
                    }
                    reportRendering("currency-input.loading")
                }
            }
        )
    }
}
