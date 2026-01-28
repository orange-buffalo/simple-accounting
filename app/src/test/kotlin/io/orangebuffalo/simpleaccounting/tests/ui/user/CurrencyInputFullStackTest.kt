package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
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
                    groups[0].options.shouldWithClue("Should have all 303 currencies in All group") {
                        shouldHaveSize(303)
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
                    groups[0].options.shouldWithClue("Should have single EUR item in shortlist") {
                        shouldContainExactlyInAnyOrder("EUREuro")
                    }
                    groups[1].name.shouldBe("All Currencies")
                    groups[1].options.shouldWithClue("All group should have all 303 currencies") {
                        shouldHaveSize(303)
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
                    groups[0].options.shouldWithClue("Should have EUR, GBP, USD in shortlist") {
                        shouldContainExactlyInAnyOrder("EUREuro", "GBPBritish Pound", "USDUS Dollar")
                    }
                    groups[1].name.shouldBe("All Currencies")
                    groups[1].options.shouldWithClue("All group should have all 303 currencies") {
                        shouldHaveSize(303)
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
                input.selectOption("EUREuro")
                input.shouldHaveSelectedValue("EUR - Euro")
                input.selectOption("GBPBritish Pound")
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
                val fry = platformUser(userName = "Fry", isAdmin = false, activated = true, documentsStorage = "noop")
                val workspace = workspace(owner = fry, defaultCurrency = "USD")
                val category = category(workspace = workspace, name = "Delivery")
                val eurExpense = expense(workspace = workspace, category = category, currency = "EUR")
            }
        }
        
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateExpensePage {
            currency {
                // Fill filter text - use "ZMW" for exact match (Zambian Kwacha)
                input.fill("ZMW")
                
                // Verify exact filtered results
                input.shouldHaveGroupedOptions { groups ->
                    val allOptions = groups.flatMap { it.options }
                    allOptions.shouldWithClue("Should have exactly one currency matching ZMW") {
                        shouldContainExactlyInAnyOrder("ZMWZambian Kwacha")
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
