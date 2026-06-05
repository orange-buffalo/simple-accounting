package io.orangebuffalo.simpleaccounting.business.ui.user.workspaces

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldBeEmpty
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.workspaces.WorkspaceAccessTokensPage.Companion.openWorkspaceAccessTokensPage
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test
import java.time.Instant

class WorkspaceAccessTokensFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should display temporary access links`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express")
                val accessToken = workspaceAccessToken(
                    workspace = workspace,
                    token = "planet-express-share",
                    validTill = MOCK_TIME.plusSeconds(10_000),
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspaceAccessTokensPage(testData.workspace.id!!) {
            shouldHaveAccessLinks(
                accessLinkRow(
                    token = testData.accessToken.token,
                    validTill = "29 Mar 1999, 1:47 am",
                )
            )
            reportRenderingWithPopovers("workspace-access-tokens.loaded")
        }
    }

    @Test
    fun `should copy temporary access link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express")
                val accessToken = workspaceAccessToken(
                    workspace = workspace,
                    token = "planet-express-share",
                    validTill = MOCK_TIME.plusSeconds(10_000),
                )
            }
        }

        page.context().grantPermissions(listOf("clipboard-read", "clipboard-write"))
        page.authenticateViaCookie(testData.fry)
        page.openWorkspaceAccessTokensPage(testData.workspace.id!!) {
            copyTemporaryAccessLink(0)
            shouldHaveNotifications { success("Temporary access link copied.") }
            shouldHaveClipboardContentForTemporaryAccessLink(testData.accessToken.token)
        }
    }

    @Test
    fun `should revoke temporary access link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express")
                val accessToken = workspaceAccessToken(
                    workspace = workspace,
                    token = "planet-express-share",
                    validTill = MOCK_TIME.plusSeconds(10_000),
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspaceAccessTokensPage(testData.workspace.id!!) {
            revokeAccessLink(0)
            reportRenderingWithPopovers("workspace-access-tokens.revoke-confirmation")
            confirmRevokeAccessLink()
            shouldHaveNoManageExistingLinksSection()
            reportRenderingWithPopovers("workspace-access-tokens.after-revoke")
        }

        aggregateTemplate.findAll<WorkspaceAccessToken>()
            .filter { it.id == testData.accessToken.id }
            .shouldBeEmpty()
    }

    @Test
    fun `should create temporary access link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express")
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openWorkspaceAccessTokensPage(testData.workspace.id!!) {
            shouldHaveNoManageExistingLinksSection()
            createTemporaryAccessLink("3025-01-15 10:00:00")
            shouldHaveNotifications { success("Temporary access link created.") }

            val createdToken = aggregateTemplate.findAll<WorkspaceAccessToken>()
                .single { it.workspaceId == testData.workspace.id }

            shouldHaveAccessLinks(
                accessLinkRow(
                    token = createdToken.token,
                    validTill = "15 Jan 3025, 10:00 am",
                )
            )

            createdToken.shouldBeEntityWithFields(
                WorkspaceAccessToken(
                    workspaceId = testData.workspace.id,
                    timeCreated = MOCK_TIME,
                    validTill = Instant.parse("3025-01-15T10:00:00Z"),
                    revoked = false,
                    token = createdToken.token,
                )
            )
        }
    }
}
