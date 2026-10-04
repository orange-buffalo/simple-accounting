package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.CreateExpensePage.Companion.openCreateExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.openExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.ExpensesOverviewPage.Companion.shouldBeExpensesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.CreateWorkspacePage.Companion.shouldBeCreateWorkspacePage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.EditWorkspacePage.Companion.shouldBeEditWorkspacePage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.openWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.shouldBeWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspaceAccessTokensPage.Companion.shouldBeWorkspaceAccessTokensPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaIconType
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItemData
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
                SaOverviewItemData(
                    title = "Planet Express",
                    primaryAttributes = listOf(
                        primaryAttribute(SaIconType.MULTI_CURRENCY, "USD"),
                        primaryAttribute(SaIconType.GLOBE, "Australia"),
                        primaryAttribute(SaIconType.SUCCESS, "Currently active"),
                    ),
                    hasDetails = false,
                ),
            )

            getWorkspacePanelByName("Planet Express")
                .shouldHaveActionMenuItems("Edit", "Manage temporary access links")

            reportRendering("workspaces-overview.single-workspace")
            reportRenderingWithPopovers("workspaces-overview.actions-menu")
        }
    }

    @Test
    fun `should navigate to workspace edit from actions menu`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, name = "Planet Express")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspacesOverviewPage {
            getWorkspacePanelByName("Planet Express").clickActionMenuItem("Edit")
        }

        page.shouldBeEditWorkspacePage {
            name { input.shouldHaveValue("Planet Express") }
        }
    }

    @Test
    fun `should navigate to temporary access links from actions menu`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    workspace(owner = it, name = "Planet Express")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspacesOverviewPage {
            getWorkspacePanelByName("Planet Express").clickActionMenuItem("Manage temporary access links")
        }

        page.shouldBeWorkspaceAccessTokensPage {
            shouldHaveNoManageExistingLinksSection()
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
                SaOverviewItemData(
                    title = "Planet Express",
                    primaryAttributes = listOf(
                        primaryAttribute(SaIconType.MULTI_CURRENCY, "USD"),
                        primaryAttribute(SaIconType.GLOBE, "Australia"),
                        primaryAttribute(SaIconType.SUCCESS, "Currently active"),
                    ),
                    hasDetails = false,
                ),
                SaOverviewItemData(
                    title = "Mom's Friendly Robot Company",
                    primaryAttributes = listOf(
                        primaryAttribute(SaIconType.MULTI_CURRENCY, "EUR"),
                        primaryAttribute(SaIconType.GLOBE, "Australia"),
                    ),
                    hasDetails = false,
                ),
                SaOverviewItemData(
                    title = "Slurm Corp",
                    primaryAttributes = listOf(
                        primaryAttribute(SaIconType.MULTI_CURRENCY, "GBP"),
                        primaryAttribute(SaIconType.GLOBE, "Australia"),
                    ),
                    hasDetails = false,
                ),
            )

            reportRendering("workspaces-overview.multiple-workspaces")
            getWorkspacePanelByName("Mom's Friendly Robot Company")
                .shouldHaveActionMenuItems("Switch to this workspace", "Edit", "Manage temporary access links")
            reportRenderingWithPopovers("workspaces-overview.inactive-actions-menu")
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
            residency { input.selectOption("Germany") }
            saveButton.click()
        }

        page.shouldBeWorkspacesOverviewPage {
            pageItems.shouldHaveExactData(
                SaOverviewItemData(
                    title = "Planet Express",
                    primaryAttributes = listOf(
                        primaryAttribute(SaIconType.MULTI_CURRENCY, "USD"),
                        primaryAttribute(SaIconType.GLOBE, "Australia"),
                        primaryAttribute(SaIconType.SUCCESS, "Currently active"),
                    ),
                    hasDetails = false,
                ),
                SaOverviewItemData(
                    title = "Mom's Friendly Robot Company",
                    primaryAttributes = listOf(
                        primaryAttribute(SaIconType.MULTI_CURRENCY, "EUR"),
                        primaryAttribute(SaIconType.GLOBE, "Germany"),
                    ),
                    hasDetails = false,
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
            getWorkspacePanelByName("Mom's Friendly Robot Company").clickSwitchAction()
        }

        page.shouldHaveSideMenu().shouldHaveWorkspaceName("Mom's Friendly Robot Company")

        page.shouldHaveSideMenu().clickExpenses()

        page.shouldBeExpensesOverviewPage {
            pageItems.shouldHaveTitles()
            this.reportRendering("workspaces.switched-workspace-expenses-empty")
        }

        page.openWorkspacesOverviewPage {
            pageItems.shouldHaveExactData(
                SaOverviewItemData(
                    title = "Planet Express",
                    primaryAttributes = listOf(
                        primaryAttribute(SaIconType.MULTI_CURRENCY, "USD"),
                        primaryAttribute(SaIconType.GLOBE, "Australia"),
                    ),
                    hasDetails = false,
                ),
                SaOverviewItemData(
                    title = "Mom's Friendly Robot Company",
                    primaryAttributes = listOf(
                        primaryAttribute(SaIconType.MULTI_CURRENCY, "USD"),
                        primaryAttribute(SaIconType.GLOBE, "Australia"),
                        primaryAttribute(SaIconType.SUCCESS, "Currently active"),
                    ),
                    hasDetails = false,
                ),
            )
            getWorkspacePanelByName("Mom's Friendly Robot Company")
                .shouldHaveActionMenuItems("Edit", "Manage temporary access links")
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
            getWorkspacePanelByName("Mom's Friendly Robot Company").clickSwitchAction()
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

        val firstPageWorkspaces = (15 downTo 6).map { "Workspace $it" }
        val secondPageWorkspaces = (5 downTo 1).map { "Workspace $it" }

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
                    workspace(
                        owner = fry,
                        name = "Workspace $index",
                        createdAt = baseTime.plusSeconds(index.toLong()),
                    )
                }
            }
        }
    }
}
