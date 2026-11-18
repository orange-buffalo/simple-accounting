package io.orangebuffalo.simpleaccounting.business.ui.shared

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.openLoginPage
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.shouldBeLoginPageUk
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPageUk
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.openDashboard
import io.orangebuffalo.simpleaccounting.business.ui.shared.loginbylink.LoginByLinkPage.Companion.openLoginByLinkPage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.openWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.shouldBeWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UserPreferencesAcceptanceFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should use browser dates for a saved shared workspace and restore owned workspace preferences`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    i18nSettings = I18nSettings(locale = "en_AU", language = "en"),
                    timeZone = "America/New_York",
                ).also { workspace(owner = it, name = "Planet Express") }
                val sharedToken = workspaceAccessToken(
                    workspace = workspace(owner = platformUser(userName = "Leela"), name = "Slurm Corp"),
                    validTill = MOCK_TIME.plusSeconds(86400),
                ).also { savedWorkspaceAccessToken(owner = fry, workspaceAccessToken = it) }
            }
        }
        page.openLoginByLinkPage(testData.sharedToken.token) {}
        page.shouldBeDashboardPage()
        page.authenticateViaCookie(testData.fry)
        page.openDashboard {
            dateRangePicker.shouldHaveDateRange(startDate = LocalDate.parse("1999-01-01"), endDate = LocalDate.parse("1999-03-29"))
        }
        page.openWorkspacesOverviewPage {
            getWorkspacePanelByName("Planet Express").clickSwitchAction()
        }
        page.shouldBeDashboardPage {
            dateRangePicker.shouldHaveDateRange(startDate = LocalDate.parse("1999-01-01"), endDate = LocalDate.parse("1999-03-28"))
        }
    }

    @Test
    fun `should apply user preferences on login and persist workspace selection`(page: Page) {
        page.openLoginPage {
            loginAs(preconditions.fry)
        }

        page.shouldBeDashboardPageUk {
            dateRangePicker.shouldHaveDateRange(
                startDate = LocalDate.parse("1999-01-01"),
                endDate = LocalDate.parse("1999-03-28"),
                inputPattern = "dd.MM.uuuu",
            )
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("100,00\u00a0USD")
                shouldHaveFinalizedText("Усього 1 витрат")
            }
        }

        page.shouldHaveSideMenu().shouldHaveWorkspaceName("Planet Express")

        page.shouldHaveSideMenu().clickWorkspacesUk()

        page.shouldBeWorkspacesOverviewPage {
            getWorkspacePanelByName("Mom's Friendly Robot Company").clickSwitchActionUk()
        }

        page.shouldHaveSideMenu().shouldHaveWorkspaceName("Mom's Friendly Robot Company")

        page.shouldHaveSideMenu().clickLogoutUk()

        page.shouldBeLoginPageUk {
            loginAsUk(preconditions.fry)
        }

        page.shouldBeDashboardPageUk {
            expensesCard {
                shouldBeLoaded()
                shouldHaveAmount("50,00\u00a0USD")
                shouldHaveFinalizedText("Усього 1 витрат")
            }
        }
        page.shouldHaveSideMenu().shouldHaveWorkspaceName("Mom's Friendly Robot Company")
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                passwordHash = "qwertyHash",
                i18nSettings = I18nSettings(locale = "uk_UA", language = "uk"),
                timeZone = "America/New_York",
            ).also {
                val planetExpress = workspace(owner = it, name = "Planet Express")
                category(workspace = planetExpress).also { category ->
                    expense(
                        workspace = planetExpress,
                        category = category,
                        title = "Slurm supplies",
                        datePaid = MOCK_DATE,
                        originalAmount = 10000,
                        convertedAmounts = amountsInDefaultCurrency(10000),
                        incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        status = ExpenseStatus.FINALIZED,
                    )
                }

                val moms = workspace(owner = it, name = "Mom's Friendly Robot Company")
                category(workspace = moms).also { category ->
                    expense(
                        workspace = moms,
                        category = category,
                        title = "Robot oil",
                        datePaid = MOCK_DATE,
                        originalAmount = 5000,
                        convertedAmounts = amountsInDefaultCurrency(5000),
                        incomeTaxableAmounts = amountsInDefaultCurrency(5000),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        status = ExpenseStatus.FINALIZED,
                    )
                }
            }
        }
    }
}
