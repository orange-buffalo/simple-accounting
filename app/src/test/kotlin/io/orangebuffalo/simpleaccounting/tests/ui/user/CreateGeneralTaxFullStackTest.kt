package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateGeneralTaxPage.Companion.openCreateGeneralTaxPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.GeneralTaxesOverviewPage.Companion.shouldBeGeneralTaxesOverviewPage
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
            // Click save without filling any fields
            saveButton.click()

            // Verify all required fields show validation errors
            title {
                shouldHaveValidationError("Please provide a title")
            }
            rate {
                shouldHaveValidationError("Please provide the rate")
            }

            reportRendering("create-general-tax.validation-errors")

            // Fill title and verify only rate error remains
            title { input.fill("VAT") }
            saveButton.click()

            title {
                shouldNotHaveValidationErrors()
            }
            rate {
                shouldHaveValidationError("Please provide the rate")
            }

            // Fill rate and verify only title error remains
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
