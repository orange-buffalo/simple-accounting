package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.EditWorkspacePage.Companion.openEditWorkspacePage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.EditWorkspacePage.Companion.shouldBeEditWorkspacePage
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspacesOverviewPage.Companion.shouldBeWorkspacesOverviewPage
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test

class EditWorkspaceFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should load workspace data`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, residency = "US")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openEditWorkspacePage(testData.workspace.id!!) {
            name {
                input.shouldHaveValue("Planet Express")
            }
            defaultCurrency {
                input.shouldHaveSelectedValue("USD - US Dollar")
            }

            reportRendering("edit-workspace.loaded")
            residency { input.shouldHaveSelectedValue("United States") }
        }
    }

    @Test
    fun `should update workspace data`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express", defaultCurrency = "USD")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openEditWorkspacePage(testData.workspace.id!!) {
            name {
                input.fill("Mom's Friendly Robot Company")
            }
            defaultCurrency { input.shouldBeDisabled() }
            residency { input.selectOption("Panama") }
            saveButton.click()
        }

        page.shouldBeWorkspacesOverviewPage {
            shouldHaveWorkspaces("Mom's Friendly Robot Company")
        }

        aggregateTemplate.findSingle<Workspace>(testData.workspace.id!!)
            .shouldBeEntityWithFields(
                Workspace(
                    name = "Mom's Friendly Robot Company",
                    defaultCurrency = "USD",
                    ownerId = testData.fry.id!!,
                    residency = "PA",
                )
            )
    }

    @Test
    fun `should show warning when saving outdated workspace state`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, residency = "US")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openEditWorkspacePage(testData.workspace.id!!) {
            name { input.fill("Mom's Friendly Robot Company") }
            aggregateTemplate.save(testData.workspace.copy(name = "Planet Express changed elsewhere"))

            saveButton.click()

            shouldHaveNotifications {
                warning("This record has changed since you opened it. Reload the page and apply your changes again.")
            }
        }
        page.shouldBeEditWorkspacePage {
            name { input.shouldHaveValue("Mom's Friendly Robot Company") }
        }

        aggregateTemplate.findSingle<Workspace>(testData.workspace.id!!).name.shouldBe("Planet Express changed elsewhere")
    }

    @Test
    fun `should require correcting a residency incompatible with the existing currency`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, defaultCurrency = "USD", residency = "AU")
            }
        }
        page.authenticateViaCookie(testData.fry)
        page.openEditWorkspacePage(testData.workspace.id!!) {
            residency { input.shouldBeEmpty() }
            saveButton.click()
            shouldHaveNotifications { validationFailed() }
            residency { shouldHaveValidationError("This value is required and should not be blank") }
            residency { input.selectOption("United States") }
            saveButton.click()
        }
        page.shouldBeWorkspacesOverviewPage {
            shouldHaveWorkspaces("Planet Express")
        }
        aggregateTemplate.findSingle<Workspace>(testData.workspace.id!!).residency.shouldBe("US")
    }

    @Test
    fun `should show validation errors for invalid inputs`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express", defaultCurrency = "USD")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openEditWorkspacePage(testData.workspace.id!!) {
            name { input.fill("") }
            saveButton.click()
            shouldHaveNotifications { validationFailed() }

            name {
                shouldHaveValidationError("This value is required and should not be blank")
            }

            reportRendering("edit-workspace.validation-error-name")

            name { input.fill("x".repeat(256)) }
            saveButton.click()
            shouldHaveNotifications { validationFailed() }

            name {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }
        }
    }

    @Test
    fun `should navigate to overview on cancel`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express", defaultCurrency = "USD")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openEditWorkspacePage(testData.workspace.id!!) {
            name { input.fill("Omicron Persei 8 Logistics") }
            cancelButton.click()
        }

        page.shouldBeWorkspacesOverviewPage()

        aggregateTemplate.findSingle<Workspace>(testData.workspace.id!!)
            .name.shouldBe("Planet Express")
    }
}
