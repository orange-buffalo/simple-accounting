package io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.CreateGeneralTaxPage.Companion.shouldBeCreateGeneralTaxPage
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.EditGeneralTaxPage.Companion.shouldBeEditGeneralTaxPage
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.GeneralTaxesOverviewPage.Companion.openGeneralTaxesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.GeneralTaxesOverviewPage.Companion.shouldBeGeneralTaxesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaActionLink
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItemData
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import org.junit.jupiter.api.Test

class GeneralTaxesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display general taxes in overview`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)

                init {
                    generalTax(workspace = workspace, title = "VAT", rateInBps = 20_00, description = "Value Added Tax")
                    generalTax(workspace = workspace, title = "Sales Tax", rateInBps = 15_00)
                    generalTax(
                        workspace = workspace,
                        title = "GST",
                        rateInBps = 10_00,
                        description = "Goods and Services Tax"
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)

        page.withBlockedApiResponse(
            "**/general-taxes*",
            initiator = {
                page.openGeneralTaxesOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeGeneralTaxesOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("general-taxes-overview.loading")
                }
            }
        )

        page.shouldBeGeneralTaxesOverviewPage {
            pageItems {
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "GST (10%)",
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                    ),
                    SaOverviewItemData(
                        title = "Sales Tax (15%)",
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "VAT (20%)",
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                    )
                )
            }

            reportRendering("general-taxes-overview.loaded")
        }
    }

    @Test
    fun `should navigate from overview to create page`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openGeneralTaxesOverviewPage {
            createButton.click()
        }

        page.shouldBeCreateGeneralTaxPage()
    }

    @Test
    fun `should navigate from overview to edit page`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    generalTax(workspace = workspace, title = "VAT", rateInBps = 20_00)
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openGeneralTaxesOverviewPage {
            pageItems {
                shouldHaveTitles("VAT (20%)")
                staticItems[0].executeEditAction()
            }
        }

        page.shouldBeEditGeneralTaxPage {
            title {
                input.shouldHaveValue("VAT")
            }
        }
    }
}
