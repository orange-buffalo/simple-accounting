package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditGeneralTaxPage.Companion.shouldBeEditGeneralTaxPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.GeneralTaxesOverviewPage.Companion.shouldBeGeneralTaxesOverviewPage
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
                ),
                ignoredProperties = arrayOf(
                    GeneralTax::id,
                    GeneralTax::version,
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
            // Clear all fields and verify validation errors
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

            // Fill title and verify only rate error remains
            title { input.fill("Sales Tax") }
            saveButton.click()

            title {
                shouldNotHaveValidationErrors()
            }
            rate {
                shouldHaveValidationError("Please provide the rate")
            }

            // Fill rate and verify only title error remains
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
