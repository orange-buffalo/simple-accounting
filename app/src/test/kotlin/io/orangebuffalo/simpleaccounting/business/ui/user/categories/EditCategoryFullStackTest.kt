package io.orangebuffalo.simpleaccounting.business.ui.user.categories

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.categories.CategoriesOverviewPage.Companion.shouldBeCategoriesOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.categories.EditCategoryPage.Companion.shouldBeEditCategoryPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test

class EditCategoryFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load category data`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(
                    workspace = workspace,
                    name = "Slurm supplies",
                    income = true,
                    expense = false,
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/categories/${testData.category.id}/edit")
        page.shouldBeEditCategoryPage {
            name {
                input.shouldHaveValue("Slurm supplies")
            }
            income.shouldBeChecked()
            expense.shouldNotBeChecked()

            reportRendering("edit-category.loaded")
        }
    }

    @Test
    fun `should update category data`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(
                    workspace = workspace,
                    name = "Slurm supplies",
                    description = "Addictive beverage costs",
                    income = true,
                    expense = false,
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/categories/${testData.category.id}/edit")
        page.shouldBeEditCategoryPage {
            name {
                input.shouldHaveValue("Slurm supplies")
                input.fill("Robot maintenance")
            }
            description {
                input.shouldHaveValue("Addictive beverage costs")
                input.fill("Oil and parts for robots")
            }
            income.click()
            expense.click()
            saveButton.click()
        }

        page.shouldBeCategoriesOverviewPage()

        aggregateTemplate.findSingle<Category>(testData.category.id!!)
            .shouldBeEntityWithFields(
                Category(
                    name = "Robot maintenance",
                    description = "Oil and parts for robots",
                    workspaceId = testData.workspace.id!!,
                    income = false,
                    expense = true,
                )
            )
    }

    @Test
    fun `should show validation errors for invalid inputs`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(
                    workspace = workspace,
                    name = "Slurm supplies",
                    income = true,
                    expense = false,
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/categories/${testData.category.id}/edit")
        page.shouldBeEditCategoryPage {
            name { input.fill("") }
            saveButton.click()

            name {
                shouldHaveValidationError("This value is required and should not be blank")
            }

            reportRendering("edit-category.validation-error-name")
            shouldHaveNotifications { validationFailed() }

            name { input.fill("x".repeat(256)) }
            saveButton.click()

            name {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }
            shouldHaveNotifications { validationFailed() }

            name { input.fill("Valid name") }
            description { input.fill("x".repeat(1001)) }
            saveButton.click()

            description {
                shouldHaveValidationError("The length of this value should be no longer than 1,000 characters")
            }
            shouldHaveNotifications { validationFailed() }
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(
                    workspace = workspace,
                    name = "Slurm supplies",
                    income = true,
                    expense = false,
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/categories/${testData.category.id}/edit")
        page.shouldBeEditCategoryPage {
            name { input.fill("Updated Name") }
            cancelButton.click()
        }

        page.shouldBeCategoriesOverviewPage()

        aggregateTemplate.findSingle<Category>(testData.category.id!!)
            .name.shouldBe("Slurm supplies")
    }
}
