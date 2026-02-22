package io.orangebuffalo.simpleaccounting.business.ui.user.customers

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.CreateCustomerPage.Companion.shouldBeCreateCustomerPage
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.CustomersOverviewPage.Companion.openCustomersOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.CustomersOverviewPage.Companion.shouldBeCustomersOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.EditCustomerPage.Companion.shouldBeEditCustomerPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaActionLink
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItemData
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import org.junit.jupiter.api.Test

class CustomersOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display customers in overview`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)

                init {
                    customer(workspace = workspace, name = "Slurm Corp")
                    customer(workspace = workspace, name = "Mom's Friendly Robot Company")
                    customer(workspace = workspace, name = "Planet Express Competitors Inc")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)

        page.withBlockedApiResponse(
            "**/customers*",
            initiator = {
                page.openCustomersOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeCustomersOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("customers-overview.loading")
                }
            }
        )

        page.shouldBeCustomersOverviewPage {
            pageItems {
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "Planet Express Competitors Inc",
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Mom's Friendly Robot Company",
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Slurm Corp",
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    )
                )
            }

            reportRendering("customers-overview.loaded")
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
        page.openCustomersOverviewPage {
            createButton.click()
        }

        page.shouldBeCreateCustomerPage()
    }

    @Test
    fun `should navigate from overview to edit page`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    customer(workspace = workspace, name = "Slurm Corp")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openCustomersOverviewPage {
            pageItems {
                shouldHaveTitles("Slurm Corp")
                staticItems[0].executeEditAction()
            }
        }

        page.shouldBeEditCustomerPage {
            name {
                input.shouldHaveValue("Slurm Corp")
            }
        }
    }
}
