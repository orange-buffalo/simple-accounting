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
    fun `should validate required fields`(page: Page) {
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
            // Test empty state - clear all fields
            title { input.fill("") }
            rate { input.fill("") }
            saveButton.click()

            title {
                shouldHaveValidationError("Please provide a title")
            }
            rate {
                shouldHaveValidationError("Please provide the rate")
            }

            reportRendering("edit-general-tax.validation-errors")

            // Test with only title filled - rate should still have error
            title { input.fill("Sales Tax") }
            saveButton.click()

            title {
                shouldNotHaveValidationErrors()
            }
            rate {
                shouldHaveValidationError("Please provide the rate")
            }

            // Clear title and fill rate - title should have error
            title { input.fill("") }
            rate { input.fill("1500") }
            saveButton.click()

            title {
                shouldHaveValidationError("Please provide a title")
            }
            rate {
                shouldNotHaveValidationErrors()
            }
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
