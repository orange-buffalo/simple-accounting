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
            saveButton.click()
        }

        page.shouldBeWorkspacesOverviewPage()

        val newWorkspace = aggregateTemplate.findAll<Workspace>()
            .single { it.name == "Mom's Friendly Robot Company" }
        newWorkspace.shouldBeEntityWithFields(
            Workspace(
                name = "Mom's Friendly Robot Company",
                defaultCurrency = "EUR",
                ownerId = preconditions.fry.id!!,
            )
        )
    }

    @Test
    fun `should show validation errors for invalid inputs`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateWorkspacePage {
            name { input.fill("") }
            saveButton.click()

            name {
                shouldHaveValidationError("This value is required and should not be blank")
            }

            reportRendering("create-workspace.validation-error-name")
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
        page.authenticateViaCookie(preconditions.fry)
        page.openCreateWorkspacePage {
            name { input.fill("Omicron Persei 8 Logistics") }
            cancelButton.click()
        }

        page.shouldBeWorkspacesOverviewPage()
    }
}
