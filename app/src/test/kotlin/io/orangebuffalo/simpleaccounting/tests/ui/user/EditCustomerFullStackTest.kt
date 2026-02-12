package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.CustomersOverviewPage.Companion.shouldBeCustomersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.ui.user.pages.EditCustomerPage.Companion.shouldBeEditCustomerPage
import org.junit.jupiter.api.Test

class EditCustomerFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load customer data`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Slurm Corp")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/customers/${testData.customer.id}/edit")
        page.shouldBeEditCustomerPage {
            name {
                input.shouldHaveValue("Slurm Corp")
            }

            reportRendering("edit-customer.loaded")
        }
    }

    @Test
    fun `should update customer data`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Slurm Corp")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/customers/${testData.customer.id}/edit")
        page.shouldBeEditCustomerPage {
            name { input.fill("Mom's Friendly Robot Company") }
            saveButton.click()
        }

        page.shouldBeCustomersOverviewPage()

        aggregateTemplate.findSingle<Customer>(testData.customer.id!!)
            .shouldBeEntityWithFields(
                Customer(
                    name = "Mom's Friendly Robot Company",
                    workspaceId = testData.workspace.id!!,
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
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Slurm Corp")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/customers/${testData.customer.id}/edit")
        page.shouldBeEditCustomerPage {
            name { input.fill("") }
            saveButton.click()

            name {
                shouldHaveErrorMessage("Please select a name")
            }

            reportRendering("edit-customer.validation-error")
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer = customer(workspace = workspace, name = "Slurm Corp")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/settings/customers/${testData.customer.id}/edit")
        page.shouldBeEditCustomerPage {
            name { input.fill("Updated Name") }
            cancelButton.click()
        }

        page.shouldBeCustomersOverviewPage()

        // Verify data was not updated
        aggregateTemplate.findSingle<Customer>(testData.customer.id!!)
            .name.shouldBe("Slurm Corp")
    }
}
