package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.integrations.WiseSetupPage.Companion.openWiseSetupPage
import io.orangebuffalo.simpleaccounting.business.ui.user.reporting.ReportingPage.Companion.openReportingPage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaWizardFullStackTest : SaFullStackTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
        }
    }

    @Test
    fun `should navigate with headers and bottom actions while guarding incomplete steps`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openReportingPage {
            selectReportStep.shouldBeOpen()
            selectDatesStep.shouldBeDisabled()
            viewReportStep.shouldBeDisabled()
            selectGeneralTaxReport()
            selectDatesStep.shouldBeOpen()
            nextButton.shouldBeDisabled()
            viewReportStep.shouldBeDisabled()
            inlineCalendar {
                selectRange(start = LocalDate.of(1999, 1, 1), end = LocalDate.of(1999, 12, 31))
            }
            nextButton.shouldBeEnabled()
            viewReportStep.click()
            viewReportStep.shouldBeOpen()
            collectedSection.shouldBeVisible()
            backButton.click()
            selectDatesStep.shouldBeOpen()
            inlineCalendar {
                shouldHaveSelectedRange(start = LocalDate.of(1999, 1, 1), end = LocalDate.of(1999, 12, 31))
                selectDay(monthIndex = 0, day = 2)
            }
            nextButton.shouldBeDisabled()
            viewReportStep.shouldBeDisabled()
            inlineCalendar {
                selectDay(monthIndex = 1, day = 10)
            }
            nextButton.shouldBeEnabled()
            backButton.click()
            selectReportStep.shouldBeOpen()
            selectIncomeTaxReport()
            nextButton.click()
            viewReportStep.shouldBeOpen()
            incomeTaxReport.incomes.shouldBeVisible()
            selectReportStep.click()
            selectReportStep.shouldBeOpen()
        }
    }

    @Test
    fun `should lock navigation during asynchronous actions and restore it afterwards`(page: Page) {
        WiseApiMocks.setupAccounts()
        page.authenticateViaCookie(preconditions.fry)
        page.openWiseSetupPage {
            tokenStep.shouldBeOpen()
            accountsStep.shouldBeDisabled()
            finishStep.shouldBeDisabled()
            next.shouldBeDisabled()
            token { input.fill(WiseApiMocks.TOKEN) }
            page.withBlockedGqlApiResponse(
                queryOrMutationName = "listWiseAccounts",
                initiator = { next.click() },
                blockedRequestSpec = {
                    status.shouldBeRegular("Verifying token and loading accounts…")
                    tokenStep.shouldBeDisabled()
                    accountsStep.shouldBeDisabled()
                    finishStep.shouldBeDisabled()
                    back.shouldBeDisabled()
                    next.shouldBeDisabled()
                },
            )
            accountsStep.shouldBeOpen()
            back.shouldBeEnabled()
            next.shouldBeDisabled()
            back.click()
            tokenStep.shouldBeOpen()
            token { input.shouldHaveValue(WiseApiMocks.TOKEN) }
            accountsStep.click()
            accountsStep.shouldBeOpen()
        }
    }
}
