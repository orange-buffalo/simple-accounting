package io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.CreateGeneralTaxPage.Companion.openCreateGeneralTaxPage
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.GeneralTaxesOverviewPage.Companion.shouldBeGeneralTaxesOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test

class CreateGeneralTaxFullStackTest : SaFullStackTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    @Test
    fun `should create a new general tax`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateGeneralTaxPage {
            title { input.fill("VAT") }
            rate { input.fill("2000") }
            description { input.fill("Value Added Tax") }
            saveButton.click()
        }

        page.shouldBeGeneralTaxesOverviewPage()

        aggregateTemplate.findSingle<GeneralTax>()
            .shouldBeEntityWithFields(
                GeneralTax(
                    title = "VAT",
                    rateInBps = 2000,
                    description = "Value Added Tax",
                    workspaceId = preconditions.workspace.id!!,
                ),
                ignoredProperties = arrayOf(
                    GeneralTax::id,
                    GeneralTax::version,
                )
            )
    }

    @Test
    fun `should validate required fields`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateGeneralTaxPage {
            // Test empty state - both fields empty
            saveButton.click()

            title {
                shouldHaveValidationError("Please provide a title")
            }
            rate {
                shouldHaveValidationError("Please provide the rate")
            }

            reportRendering("create-general-tax.validation-errors")

            // Test with only title filled - rate should still have error
            title { input.fill("VAT") }
            saveButton.click()

            title {
                shouldNotHaveValidationErrors()
            }
            rate {
                shouldHaveValidationError("Please provide the rate")
            }

            // Clear title and fill rate - title should have error
            title { input.fill("") }
            rate { input.fill("2000") }
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
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateGeneralTaxPage {
            title { input.fill("VAT") }
            rate { input.fill("2000") }
            cancelButton.click()
        }

        page.shouldBeGeneralTaxesOverviewPage()
    }
}
