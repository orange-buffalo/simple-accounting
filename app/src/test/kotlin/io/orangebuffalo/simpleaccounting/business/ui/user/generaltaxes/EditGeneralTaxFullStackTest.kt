package io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.EditGeneralTaxPage.Companion.shouldBeEditGeneralTaxPage
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.GeneralTaxesOverviewPage.Companion.shouldBeGeneralTaxesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test

class EditGeneralTaxFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load general tax data`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val tax = generalTax(
                    workspace = workspace,
                    title = "VAT",
                    rateInBps = 2000,
                    description = "Value Added Tax"
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/general-taxes/${testData.tax.id}/edit")
        page.shouldBeEditGeneralTaxPage {
            title {
                input.shouldHaveValue("VAT")
            }
            rate {
                input.shouldHaveValue("2000")
            }
            description {
                input.shouldHaveValue("Value Added Tax")
            }

            reportRendering("edit-general-tax.loaded")
        }
    }

    @Test
    fun `should update general tax data`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val tax = generalTax(
                    workspace = workspace,
                    title = "VAT",
                    rateInBps = 2000,
                    description = "Value Added Tax"
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/general-taxes/${testData.tax.id}/edit")
        page.shouldBeEditGeneralTaxPage {
            title {
                input.shouldHaveValue("VAT")
                input.fill("Sales Tax")
            }
            rate {
                input.shouldHaveValue("2000")
                input.fill("1500")
            }
            description {
                input.shouldHaveValue("Value Added Tax")
                input.fill("State Sales Tax")
            }
            saveButton.click()
        }

        page.shouldBeGeneralTaxesOverviewPage()

        aggregateTemplate.findSingle<GeneralTax>(testData.tax.id!!)
            .shouldBeEntityWithFields(
                GeneralTax(
                    title = "Sales Tax",
                    rateInBps = 1500,
                    description = "State Sales Tax",
                    workspaceId = testData.workspace.id!!,
                )
            )
    }

    @Test
    fun `should validate all fields`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val tax = generalTax(
                    workspace = workspace,
                    title = "VAT",
                    rateInBps = 2000
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/general-taxes/${testData.tax.id}/edit")
        page.shouldBeEditGeneralTaxPage {
            title { input.fill("") }
            rate { input.fill("") }
            saveButton.click()

            // Rate is null: variable coercion error fires before execution, preventing JSR-303 from running
            rate {
                shouldHaveValidationError("This value is required")
            }
            shouldHaveNotifications { validationFailed() }

            reportRendering("edit-general-tax.validation-errors-empty")

            // Fill rate to allow title to reach server-side validation
            rate { input.fill("1500") }
            saveButton.click()

            title {
                shouldHaveValidationError("This value is required and should not be blank")
            }
            rate {
                shouldNotHaveValidationErrors()
            }
            shouldHaveNotifications { validationFailed() }

            title { input.fill("Sales Tax") }
            rate { input.fill("") }
            saveButton.click()

            title {
                shouldNotHaveValidationErrors()
            }
            rate {
                shouldHaveValidationError("This value is required")
            }
            shouldHaveNotifications { validationFailed() }

            title { input.fill("x".repeat(256)) }
            rate { input.fill("1500") }
            saveButton.click()

            title {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }
            shouldHaveNotifications { validationFailed() }

            title { input.fill("VAT") }
            description { input.fill("x".repeat(256)) }
            saveButton.click()

            description {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }
            shouldHaveNotifications { validationFailed() }

            description { input.fill("") }
            rate { input.fill("-1") }
            saveButton.click()

            rate {
                shouldHaveValidationError("The value must be no less than 0")
            }
            shouldHaveNotifications { validationFailed() }

            rate { input.fill("10001") }
            saveButton.click()

            rate {
                shouldHaveValidationError("The value must be no greater than 10,000")
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
                val tax = generalTax(
                    workspace = workspace,
                    title = "VAT",
                    rateInBps = 2000
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/general-taxes/${testData.tax.id}/edit")
        page.shouldBeEditGeneralTaxPage {
            title { input.fill("Updated Name") }
            cancelButton.click()
        }

        page.shouldBeGeneralTaxesOverviewPage()

        // Verify data was not updated
        aggregateTemplate.findSingle<GeneralTax>(testData.tax.id!!)
            .title.shouldBe("VAT")
    }
}
