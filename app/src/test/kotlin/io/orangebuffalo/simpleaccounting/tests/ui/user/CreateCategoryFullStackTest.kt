package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CategoriesOverviewPage.Companion.shouldBeCategoriesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateCategoryPage.Companion.openCreateCategoryPage
import org.junit.jupiter.api.Test

class CreateCategoryFullStackTest : SaFullStackTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    @Test
    fun `should create a new category with income and expense`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCategoryPage {
            name { input.fill("Slurm supplies") }
            description {
                input.fill("Addictive beverage costs")
            }
            income.click()
            expense.click()
            saveButton.click()
        }

        page.shouldBeCategoriesOverviewPage()

        aggregateTemplate.findSingle<Category>()
            .shouldBeEntityWithFields(
                Category(
                    name = "Slurm supplies",
                    description = "Addictive beverage costs",
                    workspaceId = preconditions.workspace.id!!,
                    income = true,
                    expense = true
                ),
                ignoredProperties = arrayOf(
                    Category::id,
                    Category::version,
                )
            )
    }

    @Test
    fun `should create a category with income only`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCategoryPage {
            name { input.fill("Robot maintenance") }
            income.click()
            saveButton.click()
        }

        page.shouldBeCategoriesOverviewPage()

        aggregateTemplate.findSingle<Category>()
            .shouldBeEntityWithFields(
                Category(
                    name = "Robot maintenance",
                    workspaceId = preconditions.workspace.id!!,
                    income = true,
                    expense = false
                ),
                ignoredProperties = arrayOf(
                    Category::id,
                    Category::version,
                    Category::description,
                )
            )
    }

    @Test
    fun `should show validation error for empty name`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCategoryPage {
            income.click()
            saveButton.click()

            name {
                shouldHaveValidationError("Please input name")
            }

            reportRendering("create-category.validation-error-name")
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCategoryPage {
            name { input.fill("Slurm supplies") }
            cancelButton.click()
        }

        page.shouldBeCategoriesOverviewPage()
    }
}
