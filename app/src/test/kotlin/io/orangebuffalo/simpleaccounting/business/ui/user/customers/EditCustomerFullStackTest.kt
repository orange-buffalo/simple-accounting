package io.orangebuffalo.simpleaccounting.business.ui.user.customers

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.CustomersOverviewPage.Companion.shouldBeCustomersOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.customers.EditCustomerPage.Companion.shouldBeEditCustomerPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
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
            name {
                input.shouldHaveValue("Slurm Corp")
                input.fill("Mom's Friendly Robot Company")
            }
            saveButton.click()
        }

        page.shouldBeCustomersOverviewPage()

        aggregateTemplate.findSingle<Customer>(testData.customer.id!!)
            .shouldBeEntityWithFields(
                Customer(
                    name = "Mom's Friendly Robot Company",
                    workspaceId = testData.workspace.id!!,
                )
            )
    }

    @Test
    fun `should show validation errors for invalid name`(page: Page) {
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
                shouldHaveValidationError("This value is required and should not be blank")
            }
            shouldHaveNotifications { validationFailed() }

            reportRendering("edit-customer.validation-error-name-empty")

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
