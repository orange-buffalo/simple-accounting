package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.CreateWorkspacePage.Companion.openCreateWorkspacePage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.shouldBeWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test

class CreateWorkspaceFullStackTest : SaFullStackTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
        }
    }

    @Test
    fun `should create a new workspace`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateWorkspacePage {
            name { input.fill("Mom's Friendly Robot Company") }
            defaultCurrency { input.selectOption("EUREuro") }
            residency {
                input.selectOption("Germany")
            }
            saveButton.click()
        }

        page.shouldBeWorkspacesOverviewPage {
            shouldHaveWorkspaces("Planet Express", "Mom's Friendly Robot Company")
        }

        val newWorkspace = aggregateTemplate.findAll<Workspace>()
            .single { it.name == "Mom's Friendly Robot Company" }
        newWorkspace.shouldBeEntityWithFields(
            Workspace(
                name = "Mom's Friendly Robot Company",
                defaultCurrency = "EUR",
                ownerId = preconditions.fry.id!!,
                residency = "DE",
            )
        )
    }

    @Test
    fun `should disable residency until currency is selected and reset it on currency change`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateWorkspacePage {
            residency {
                input.shouldBeDisabled()
                input.shouldHavePlaceholder("Select a currency first")
            }
            defaultCurrency { input.selectOption("AUDAustralian Dollar") }
            residency {
                input.shouldHaveOptions(
                    "Australia", "Christmas Island", "Cocos (Keeling) Islands", "Heard & McDonald Islands",
                    "Kiribati", "Nauru", "Norfolk Island", "Tuvalu",
                )
                input.selectOption("Australia")
            }
            defaultCurrency { input.selectOption("USDUS Dollar") }
            residency {
                input.shouldBeEmpty()
                input.shouldHaveOptions(
                    "American Samoa", "British Indian Ocean Territory", "British Virgin Islands", "Caribbean Netherlands", "Ecuador",
                    "Guam", "Haiti", "Marshall Islands", "Micronesia", "Northern Mariana Islands", "Palau", "Panama",
                    "Puerto Rico", "Timor-Leste", "Turks & Caicos Islands", "U.S. Outlying Islands",
                    "U.S. Virgin Islands", "United States",
                )
                input.selectOption("United States")
            }
        }
    }

    @Test
    fun `should show validation errors for invalid inputs`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateWorkspacePage {
            name { input.fill("") }
            saveButton.click()
            shouldHaveNotifications { validationFailed() }

            name {
                shouldHaveValidationError("This value is required and should not be blank")
            }

            reportRendering("create-workspace.validation-error-name")

            name { input.fill("x".repeat(256)) }
            defaultCurrency { input.selectOption("AUDAustralian Dollar") }
            residency { input.selectOption("Australia") }
            saveButton.click()
            shouldHaveNotifications { validationFailed() }

            name {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateWorkspacePage {
            name { input.fill("Omicron Persei 8 Logistics") }
            cancelButton.click()
        }

        page.shouldBeWorkspacesOverviewPage()
    }
}
