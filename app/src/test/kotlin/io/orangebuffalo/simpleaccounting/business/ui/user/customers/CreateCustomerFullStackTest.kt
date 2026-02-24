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
    fun `should show validation error for empty name`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCustomerPage {
            saveButton.click()

            name {
                shouldHaveValidationError("Please provide a name")
            }

            reportRendering("create-customer.validation-error")
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
