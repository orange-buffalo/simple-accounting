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
                val workspace = workspace(owner = fry, name = "Planet Express", defaultCurrency = "USD")
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
            saveButton.click()
        }

        page.shouldBeWorkspacesOverviewPage()

        aggregateTemplate.findSingle<Workspace>(testData.workspace.id!!)
            .shouldBeEntityWithFields(
                Workspace(
                    name = "Mom's Friendly Robot Company",
                    defaultCurrency = "USD",
                    ownerId = testData.fry.id!!,
                )
            )
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

            name {
                shouldHaveValidationError("This value is required and should not be blank")
            }

            reportRendering("edit-workspace.validation-error-name")
            shouldHaveNotifications { validationFailed() }

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
            .name shouldBe "Planet Express"
    }
}
