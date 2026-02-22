package io.orangebuffalo.simpleaccounting.business.ui.user.categories

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.categories.CategoriesOverviewPage.Companion.openCategoriesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.categories.CategoriesOverviewPage.Companion.shouldBeCategoriesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.categories.CreateCategoryPage.Companion.shouldBeCreateCategoryPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItemData
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import org.junit.jupiter.api.Test

class CategoriesOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display categories in overview`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)

                init {
                    category(workspace = workspace, name = "Slurm supplies", income = true, expense = false)
                    category(workspace = workspace, name = "Robot maintenance", income = false, expense = true)
                    category(workspace = workspace, name = "Delivery", income = true, expense = true)
                }
            }
        }

        page.authenticateViaCookie(testData.fry)

        page.withBlockedApiResponse(
            "**/categories*",
            initiator = {
                page.openCategoriesOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeCategoriesOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("categories-overview.loading")
                }
            }
        )

        page.shouldBeCategoriesOverviewPage {
            pageItems {
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "Delivery",
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Robot maintenance",
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Slurm supplies",
                        hasDetails = false,
                    )
                )
            }

            reportRendering("categories-overview.loaded")
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
        page.openCategoriesOverviewPage {
            createButton.click()
        }

        page.shouldBeCreateCategoryPage()
    }
}
