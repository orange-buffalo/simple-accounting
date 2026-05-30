package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.comparables.shouldBeLessThan
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaPageableItemsFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should debounce search argument changes and use the final typed query`(page: Page) {
        val searchQuery = "Robot oil B"
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it).also { workspace ->
                        val category = category(workspace = workspace, name = "Robot maintenance")
                        expense(
                            workspace = workspace,
                            category = category,
                            title = "Robot oil A",
                            datePaid = LocalDate.of(3025, 6, 5),
                        )
                        expense(
                            workspace = workspace,
                            category = category,
                            title = searchQuery,
                            datePaid = LocalDate.of(3025, 6, 6),
                        )
                    }
                }
            }
        }
        var pageRequests = 0
        page.onRequest { request ->
            val postData = try { request.postData() } catch (e: Exception) { null }
            if (postData?.contains("\"operationName\":\"expensesPage\"") == true) {
                pageRequests += 1
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openExpensesOverviewPage {
            pageItems {
                shouldHaveDataSatisfying { items ->
                    items.map { it.title }.shouldContainAll("Robot oil A", searchQuery)
                }
            }
            pageRequests = 0

            filters.addTextFilter("Title, category, notes", searchQuery)
            pageItems {
                shouldHaveTitles(searchQuery)
            }
        }

        // We intentionally do not assert an exact count: framework internals may legitimately trigger
        // one extra query, but debounce must still prevent one API request per typed character.
        pageRequests.shouldBeLessThan(searchQuery.length)
    }
}
