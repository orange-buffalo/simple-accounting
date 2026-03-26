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
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test

class WorkspacesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display single workspace`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, name = "Planet Express")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspacesOverviewPage {
            pageItems.shouldHaveExactData(
                WorkspacePanelData(
                    title = "Planet Express",
                    switchButtonVisible = false,
                    defaultCurrency = "USD",
                ),
            )

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
            pageItems.shouldHaveExactData(
                WorkspacePanelData(
                    title = "Planet Express",
                    switchButtonVisible = false,
                    defaultCurrency = "USD",
                ),
                WorkspacePanelData(
                    title = "Mom's Friendly Robot Company",
                    switchButtonVisible = true,
                    defaultCurrency = "EUR",
                ),
                WorkspacePanelData(
                    title = "Slurm Corp",
                    switchButtonVisible = true,
                    defaultCurrency = "GBP",
                ),
            )

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

        page.openWorkspacesOverviewPage {
            createButton.click()
        }

        page.shouldBeCreateWorkspacePage()

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
            pageItems.shouldHaveExactData(
                WorkspacePanelData(
                    title = "Planet Express",
                    switchButtonVisible = false,
                    defaultCurrency = "USD",
                ),
                WorkspacePanelData(
                    title = "Mom's Friendly Robot Company",
                    switchButtonVisible = true,
                    defaultCurrency = "EUR",
                ),
            )
        }
    }

    @Test
    fun `should switch workspace and verify data isolation`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, name = "Planet Express").also { ws ->
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

        page.openExpensesOverviewPage {
            pageItems.shouldHaveTitles("Slurm supplies")
        }

        page.openWorkspacesOverviewPage {
            shouldHaveWorkspaces("Planet Express", "Mom's Friendly Robot Company")
            getWorkspacePanelByName("Mom's Friendly Robot Company").clickSwitchButton()
        }

        page.shouldHaveSideMenu().shouldHaveWorkspaceName("Mom's Friendly Robot Company")

        page.shouldHaveSideMenu().clickExpenses()

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
                    category(workspace = it, name = "Robot maintenance")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)

        page.openWorkspacesOverviewPage {
            shouldHaveWorkspaces("Planet Express", "Mom's Friendly Robot Company")
            getWorkspacePanelByName("Mom's Friendly Robot Company").clickSwitchButton()
        }

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

        page.shouldBeExpensesOverviewPage()

        val createdExpense = aggregateTemplate.findSingle<Expense>()
        createdExpense.workspaceId.shouldBe(testData.moms.id)
        createdExpense.originalAmount.shouldBe(10000)
        createdExpense.title.shouldBe("Robot oil")
    }

    @Test
    fun `should support pagination`(page: Page) {
        page.authenticateViaCookie(preconditionsPagination.fry)

        val firstPageWorkspaces = (1..10).map { "Workspace $it" }
        val secondPageWorkspaces = (11..15).map { "Workspace $it" }

        page.openWorkspacesOverviewPage {
            shouldHaveWorkspaces(*firstPageWorkspaces.toTypedArray())
            pageItems.paginator {
                shouldHaveActivePage(1)
                shouldHaveTotalPages(2)
                next()
                shouldHaveActivePage(2)
                shouldHaveTotalPages(2)
            }
            shouldHaveWorkspaces(*secondPageWorkspaces.toTypedArray())
            pageItems.paginator {
                previous()
                shouldHaveActivePage(1)
                shouldHaveTotalPages(2)
            }
            shouldHaveWorkspaces(*firstPageWorkspaces.toTypedArray())
        }
    }

    private val preconditionsPagination by lazyPreconditions {
        object {
            val fry = fry()

            init {
                val baseTime = MOCK_TIME.plusSeconds(100)
                (1..15).forEach { index ->
                    Workspace(
                        name = "Workspace $index",
                        ownerId = fry.id!!,
                        defaultCurrency = "USD",
                    ).also {
                        it.createdAt = baseTime.plusSeconds(index.toLong())
                    }.save()
                }
            }
        }
    }
}
