package io.orangebuffalo.simpleaccounting.business.ui.user.customers

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.CreateCustomerPage.Companion.openCreateCustomerPage
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.CustomersOverviewPage.Companion.shouldBeCustomersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test

class CreateCustomerFullStackTest : SaFullStackTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    @Test
    fun `should create a new customer`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCustomerPage {
            name { input.fill("Slurm Corp") }
            saveButton.click()
        }

        page.shouldBeCustomersOverviewPage()

        aggregateTemplate.findSingle<Customer>()
            .shouldBeEntityWithFields(
                Customer(
                    name = "Slurm Corp",
                    workspaceId = preconditions.workspace.id!!,
                )
            )
    }

    @Test
    fun `should show validation errors for invalid name`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCustomerPage {
            saveButton.click()

            name {
                shouldHaveValidationError("This value is required and should not be blank")
            }
            shouldHaveNotifications { validationFailed() }

            reportRendering("create-customer.validation-error-name-empty")

            name { input.fill("x".repeat(256)) }
            saveButton.click()

            name {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }
            shouldHaveNotifications { validationFailed() }
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCustomerPage {
            name { input.fill("Slurm Corp") }
            cancelButton.click()
        }

        page.shouldBeCustomersOverviewPage()
    }
}
