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
                )
            )
    }

    @Test
    fun `should validate all fields`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateGeneralTaxPage {
            saveButton.click()

            // Rate is null: client-side validation fires first, only rate error is shown
            rate {
                shouldHaveValidationError("This value is required")
            }
            shouldHaveNotifications { validationFailed() }

            reportRendering("create-general-tax.validation-errors-empty")

            // Fill rate to allow title to reach server-side validation
            rate { input.fill("2000") }
            saveButton.click()

            title {
                shouldHaveValidationError("This value is required and should not be blank")
            }
            rate {
                shouldNotHaveValidationErrors()
            }
            shouldHaveNotifications { validationFailed() }

            title { input.fill("VAT") }
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
            rate { input.fill("2000") }
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

            reportRendering("create-general-tax.validation-error-rate-min")

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
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateGeneralTaxPage {
            title { input.fill("VAT") }
            rate { input.fill("2000") }
            cancelButton.click()
        }

        page.shouldBeGeneralTaxesOverviewPage()
    }
}
