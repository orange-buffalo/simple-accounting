package io.orangebuffalo.simpleaccounting.business.ui.user.incomes

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomePage.Companion.shouldBeEditIncomePage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EditIncomeFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should show warning when saving outdated income state`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val income = income(workspace = workspace)
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/incomes/${testData.income.id}/edit")
        page.shouldBeEditIncomePage {
            title { input.fill("Updated Mars delivery payment") }
            aggregateTemplate.save(testData.income.copy(title = "Delivery payment changed elsewhere"))

            saveButton.click()

            shouldHaveNotifications {
                warning("This record has changed since you opened it. Reload the page and apply your changes again.")
            }
        }
        page.shouldBeEditIncomePage {
            title { input.shouldHaveValue("Updated Mars delivery payment") }
        }

        aggregateTemplate.findSingle<Income>(testData.income.id!!).title.shouldBe("Delivery payment changed elsewhere")
    }

    @Test
    fun `should validate required fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Delivery")
                val income = income(
                    workspace = workspace,
                    category = category,
                    title = "Delivery to Mars payment",
                    originalAmount = 10000L,
                    dateReceived = LocalDate.of(3025, 1, 15),
                    useDifferentExchangeRateForIncomeTaxPurposes = false
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/incomes/${testData.income.id}/edit")
        page.shouldBeEditIncomePage {
            // Sending title="" triggers @NotBlank constraint violation (field is present but blank);
            // originalAmount is still loaded so it is not missing and does not cause a coercion error.
            title { input.fill("") }
            saveButton.click()

            title {
                shouldHaveValidationError("This value is required and should not be blank")
            }
            shouldHaveNotifications { validationFailed() }

            reportRendering("edit-income.validation-errors")

            // Clearing the money input omits originalAmount from variables, causing a null-coercion
            // error on the server; title is restored so it does not interfere with this check.
            title { input.fill("Delivery to Mars payment") }
            originalAmount { input.fill("") }
            saveButton.click()

            originalAmount {
                shouldHaveValidationError("This value is required")
            }
            shouldHaveNotifications { validationFailed() }
        }
    }

    @Test
    fun `should show validation errors for constraint violations`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val category = category(workspace = workspace, name = "Delivery")
                val income = income(
                    workspace = workspace,
                    category = category,
                    title = "Slurm payment",
                    originalAmount = 5000L,
                    dateReceived = LocalDate.of(3025, 1, 15),
                    useDifferentExchangeRateForIncomeTaxPurposes = false
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/incomes/${testData.income.id}/edit")
        page.shouldBeEditIncomePage {
            title { input.fill("x".repeat(256)) }
            saveButton.click()

            title {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }
            shouldHaveNotifications { validationFailed() }

            title { input.fill("Slurm payment") }
            notes { input.fill("x".repeat(1025)) }
            saveButton.click()

            notes {
                shouldHaveValidationError("The length of this value should be no longer than 1,024 characters")
            }
            shouldHaveNotifications { validationFailed() }
        }
    }
}
