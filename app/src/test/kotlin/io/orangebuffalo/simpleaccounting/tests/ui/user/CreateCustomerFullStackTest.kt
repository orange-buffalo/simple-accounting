package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CreateCustomerPage.Companion.openCreateCustomerPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CustomersOverviewPage.Companion.shouldBeCustomersOverviewPage
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
                ),
                ignoredProperties = arrayOf(
                    Customer::id,
                    Customer::version,
                    Customer::timeRecorded,
                )
            )
    }

    @Test
    fun `should show validation error for empty name`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateCustomerPage {
            saveButton.click()

            name {
                shouldHaveErrorMessage("Please select a name")
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
