package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.WorkspaceEditorPage.Companion.shouldBeCreateWorkspacePage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.WorkspacesOverviewPage.Companion.openWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.WorkspacesOverviewPage.Companion.shouldBeWorkspacesOverviewPage
import org.junit.jupiter.api.Test

class WorkspacesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display single current workspace`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express")
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
                val fry = fry()
                val planetExpress = workspace(owner = fry, name = "Planet Express")
                val moms = workspace(owner = fry, name = "Mom's Friendly Robot Company")
                val slurm = workspace(owner = fry, name = "Slurm Corp")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspacesOverviewPage {
            shouldHaveCurrentWorkspace {
                shouldHaveTitle("Planet Express")
                shouldNotHaveSwitchButton()
            }
            shouldHaveOtherWorkspaces(2) {
                it.shouldHaveSwitchButton()
            }
            
            reportRendering("workspaces-overview.multiple-workspaces")
        }
    }

    @Test
    fun `should navigate to create workspace page`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspacesOverviewPage {
            createButton.click()
        }

        page.shouldBeCreateWorkspacePage()
    }

    @Test
    fun `should create new workspace`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/workspaces/create")
        
        page.shouldBeCreateWorkspacePage {
            name {
                input.fill("Mom's Friendly Robot Company")
            }
            defaultCurrency {
                input.selectOption("EUREuro")
            }
            saveButton.click()
        }

        page.shouldBeWorkspacesOverviewPage {
            shouldHaveCurrentWorkspace {
                shouldHaveTitle("Planet Express")
            }
            shouldHaveOtherWorkspaces(1)
        }
    }

    @Test
    fun `should switch workspace and verify navigation menu updated`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val planetExpress = workspace(owner = fry, name = "Planet Express")
                val moms = workspace(owner = fry, name = "Mom's Friendly Robot Company")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspacesOverviewPage {
            getOtherWorkspaceByName("Mom's Friendly Robot Company").clickSwitchButton()
        }

        // Should navigate to dashboard after switching
        page.shouldHaveSideMenu().shouldHaveWorkspaceName("Mom's Friendly Robot Company")
    }

    @Test
    fun `should display empty expenses list after switching workspace`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val planetExpress = workspace(owner = fry, name = "Planet Express")
                val moms = workspace(owner = fry, name = "Mom's Friendly Robot Company")
                
                init {
                    // Create an expense in Planet Express workspace
                    val expenseCategory = category(workspace = planetExpress)
                    expense(
                        workspace = planetExpress,
                        category = expenseCategory,
                        title = "Slurm supplies",
                        originalAmount = 5000,
                        convertedAmounts = amountsInDefaultCurrency(5000),
                        incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                        useDifferentExchangeRateForIncomeTaxPurposes = false
                    )
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

        // Navigate to expenses in new workspace
        page.shouldHaveSideMenu().clickExpenses()

        // Verify expenses list is empty
        page.shouldBeExpensesOverviewPage {
            pageItems.shouldHaveTitles()
            this.reportRendering("workspaces.switched-workspace-expenses-empty")
        }
    }

    @Test
    fun `should create expense in new workspace and verify it is linked properly`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val planetExpress = workspace(owner = fry, name = "Planet Express")
                val moms = workspace(owner = fry, name = "Mom's Friendly Robot Company", defaultCurrency = "USD")
                
                init {
                    // Create category in Mom's workspace for the test
                    category(workspace = moms, name = "Robot maintenance")
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
