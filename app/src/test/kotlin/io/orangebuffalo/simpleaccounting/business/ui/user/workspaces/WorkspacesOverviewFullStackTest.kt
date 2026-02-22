package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspaceEditorPage.Companion.shouldBeCreateWorkspacePage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.openWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.shouldBeWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test

class WorkspacesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display single current workspace`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, name = "Planet Express")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspacesOverviewPage {
            shouldHaveCurrentWorkspace {
                shouldHaveTitle("Planet Express")
                shouldNotHaveSwitchButton()
            }
            shouldNotHaveOtherWorkspaces()

            reportRendering("workspaces-overview.single-workspace")
        }
    }

    @Test
    fun `should display multiple workspaces`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, name = "Planet Express", defaultCurrency = "USD")
                    workspace(owner = it, name = "Mom's Friendly Robot Company", defaultCurrency = "EUR")
                    workspace(owner = it, name = "Slurm Corp", defaultCurrency = "GBP")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspacesOverviewPage {
            shouldHaveCurrentWorkspace {
                shouldHaveTitle("Planet Express")
                shouldNotHaveSwitchButton()
            }

            // Verify both other workspaces with their actual titles and that they have switch buttons
            getOtherWorkspaceByName("Mom's Friendly Robot Company").apply {
                shouldHaveTitle("Mom's Friendly Robot Company")
                shouldHaveSwitchButton()
            }

            getOtherWorkspaceByName("Slurm Corp").apply {
                shouldHaveTitle("Slurm Corp")
                shouldHaveSwitchButton()
            }

            reportRendering("workspaces-overview.multiple-workspaces")
        }
    }

    @Test
    fun `should create new workspace`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(testData.fry)

        // Navigate to create page via button click
        page.openWorkspacesOverviewPage {
            createButton.click()
        }

        // Verify navigation to create page
        page.shouldBeCreateWorkspacePage()

        // Fill and save the form
        page.shouldBeCreateWorkspacePage {
            name {
                input.fill("Mom's Friendly Robot Company")
            }
            defaultCurrency {
                input.selectOption("EUREuro")
            }
            saveButton.click()
        }

        // Verify navigation back to overview and new workspace appears
        page.shouldBeWorkspacesOverviewPage {
            shouldHaveCurrentWorkspace {
                shouldHaveTitle("Planet Express")
            }

            // Verify the newly created workspace appears in other workspaces
            getOtherWorkspaceByName("Mom's Friendly Robot Company").apply {
                shouldHaveTitle("Mom's Friendly Robot Company")
                shouldHaveSwitchButton()
            }
        }
    }

    @Test
    fun `should switch workspace and verify data isolation`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, name = "Planet Express").also { ws ->
                        // Create an expense in Planet Express workspace
                        val expenseCategory = category(workspace = ws)
                        expense(
                            workspace = ws,
                            category = expenseCategory,
                            title = "Slurm supplies",
                            originalAmount = 5000,
                            convertedAmounts = amountsInDefaultCurrency(5000),
                            incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                            useDifferentExchangeRateForIncomeTaxPurposes = false
                        )
                    }
                    workspace(owner = it, name = "Mom's Friendly Robot Company")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)

        // Verify Planet Express has expense
        page.openExpensesOverviewPage {
            pageItems.shouldHaveTitles("Slurm supplies")
        }

        // Switch to Mom's workspace
        page.openWorkspacesOverviewPage {
            getOtherWorkspaceByName("Mom's Friendly Robot Company").clickSwitchButton()
        }

        // Verify navigation menu updated with new workspace name
        page.shouldHaveSideMenu().shouldHaveWorkspaceName("Mom's Friendly Robot Company")

        // Navigate to expenses in switched workspace
        page.shouldHaveSideMenu().clickExpenses()

        // Verify expenses list is empty - data isolation works
        page.shouldBeExpensesOverviewPage {
            pageItems.shouldHaveTitles()
            this.reportRendering("workspaces.switched-workspace-expenses-empty")
        }
    }

    @Test
    fun `should create expense in new workspace and verify it is linked properly`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, name = "Planet Express")

                }
                val moms = workspace(owner = fry, name = "Mom's Friendly Robot Company", defaultCurrency = "USD").also {
                    // Create category in Mom's workspace for the test
                    category(workspace = it, name = "Robot maintenance")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)

        // Switch to Mom's workspace
        page.openWorkspacesOverviewPage {
            getOtherWorkspaceByName("Mom's Friendly Robot Company").clickSwitchButton()
        }

        // Navigate to create expense page
        page.shouldHaveSideMenu().clickExpenses()
        page.openCreateExpensePage {
            category {
                input.selectOption("Robot maintenance")
            }
            title {
                input.fill("Robot oil")
            }
            originalAmount {
                input.fill("100.00")
            }
            saveButton.click()
        }

        // Verify navigation to overview
        page.shouldBeExpensesOverviewPage()

        // Verify expense is created and linked to Mom's workspace
        val createdExpense = aggregateTemplate.findSingle<Expense>()
        createdExpense.workspaceId.shouldBe(testData.moms.id)
        createdExpense.originalAmount.shouldBe(10000)
        createdExpense.title.shouldBe("Robot oil")
    }
}
