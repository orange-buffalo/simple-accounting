package io.orangebuffalo.simpleaccounting.services.business

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.*
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@SimpleAccountingIntegrationTest
@DisplayName("WorkspaceService ")
internal class WorkspaceServiceIT(
    @Autowired private val workspaceService: WorkspaceService,
    @Autowired private val timeService: TimeService,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @BeforeEach
    fun setup() {
        mockCurrentTime(timeService)
    }

    @Test
    fun `should fail to provide admin workspace access if user is not authenticated`() {
        val testData = setupPreconditions()
        assertThatThrownBy {
            runBlocking {
                workspaceService.getAccessibleWorkspace(testData.fryWorkspace.id!!, WorkspaceAccessMode.ADMIN)
            }
        }.hasMessage("Authentication is not set")
    }

    @Test
    fun `should fail to provide read-write workspace access if user is not authenticated`() {
        val testData = setupPreconditions()
        assertThatThrownBy {
            runBlocking {
                workspaceService.getAccessibleWorkspace(testData.fryWorkspace.id!!, WorkspaceAccessMode.READ_WRITE)
            }
        }.hasMessage("Authentication is not set")
    }

    @Test
    fun `should fail to provide read-only workspace access if user is not authenticated`() {
        val testData = setupPreconditions()
        assertThatThrownBy {
            runBlocking {
                workspaceService.getAccessibleWorkspace(testData.fryWorkspace.id!!, WorkspaceAccessMode.READ_ONLY)
            }
        }.hasMessage("Authentication is not set")
    }

    @WithMockFryUser
    @Test
    fun `should provide admin access to workspace for its owner`() {
        val testData = setupPreconditions()
        assertSuccessfulGetAccessibleWorkspace(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithMockFarnsworthUser
    @Test
    fun `should not provide admin access to workspace for users with valid token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithMockRobertoUser
    @Test
    fun `should not provide admin access to workspace for users with expired tokens`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithMockMafiaBotUser
    @Test
    fun `should not provide admin access to workspace for users with revoked tokens`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithMockZoidbergUser
    @Test
    fun `should not provide admin access to workspace for other users`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithMockFryUser
    @Test
    fun `should provide read-write access to workspace for its owner`() {
        val testData = setupPreconditions()
        assertSuccessfulGetAccessibleWorkspace(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockFarnsworthUser
    @Test
    fun `should not provide read-write access to workspace for users with valid token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockRobertoUser
    @Test
    fun `should not provide read-write access to workspace for users with expired tokens`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockMafiaBotUser
    @Test
    fun `should not provide read-write access to workspace for users with revoked tokens`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockZoidbergUser
    @Test
    fun `should not provide read-write access to workspace for other users`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockFryUser
    @Test
    fun `should provide read-only access to workspace for its owner`() {
        val testData = setupPreconditions()
        assertSuccessfulGetAccessibleWorkspace(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockFarnsworthUser
    @Test
    fun `should provide read-only access to workspace for users with valid token`() {
        val testData = setupPreconditions()
        assertSuccessfulGetAccessibleWorkspace(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockRobertoUser
    @Test
    fun `should not provide read-only access to workspace for users with expired tokens`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockMafiaBotUser
    @Test
    fun `should not provide read-only access to workspace for users with revoked tokens`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockZoidbergUser
    @Test
    fun `should not provide read-only access to workspace for other users`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }


    @WithSaMockUser(transient = true, workspaceAccessToken = "valid")
    @Test
    fun `should not provide admin access to workspace for transient user with valid token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "expired")
    @Test
    fun `should not provide admin access to workspace for transient user with expired token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "revoked")
    @Test
    fun `should not provide admin access to workspace for transient user with revoked token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "validForFarnsworth")
    @Test
    fun `should not provide admin access to workspace for transient user with valid token for other workspace`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.ADMIN)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "valid")
    @Test
    fun `should not provide read-write access to workspace for transient user with valid token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "expired")
    @Test
    fun `should not provide read-write access to workspace for transient user with expired token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "revoked")
    @Test
    fun `should not provide read-write access to workspace for transient user with revoked token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "validForFarnsworth")
    @Test
    fun `should not provide read-write access to workspace for transient user with valid token for other workspace`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_WRITE)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "valid")
    @Test
    fun `should provide read-only access to workspace for transient user with valid token`() {
        val testData = setupPreconditions()
        assertSuccessfulGetAccessibleWorkspace(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "expired")
    @Test
    fun `should not provide read-only access to workspace for transient user with expired token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "revoked")
    @Test
    fun `should not provide read-only access to workspace for transient user with revoked token`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }

    @WithSaMockUser(transient = true, workspaceAccessToken = "validForFarnsworth")
    @Test
    fun `should not provide read-only access to workspace for transient user with valid token for other workspace`() {
        val testData = setupPreconditions()
        assertThatGetAccessibleWorkspaceFailed(testData.fryWorkspace, WorkspaceAccessMode.READ_ONLY)
    }

    private fun assertThatGetAccessibleWorkspaceFailed(
        fryWorkspace: Workspace,
        mode: WorkspaceAccessMode
    ) {
        assertThatGetAccessibleWorkspace(
            fryWorkspace,
            mode, {
                isInstanceOf(EntityNotFoundException::class.java)
                    .hasMessage("Workspace ${fryWorkspace.id} is not found")
            },
            {})
    }

    private fun assertSuccessfulGetAccessibleWorkspace(
        fryWorkspace: Workspace,
        mode: WorkspaceAccessMode
    ) {
        assertThatGetAccessibleWorkspace(
            fryWorkspace,
            mode,
            { doesNotThrowAnyException() },
            { isEqualTo(fryWorkspace) })
    }

    private fun assertThatGetAccessibleWorkspace(
        fryWorkspace: Workspace,
        mode: WorkspaceAccessMode,
        exceptionAssertions: AbstractThrowableAssert<*, *>.() -> Unit,
        workspaceAssertions: Assert<Workspace?>.() -> Unit
    ) {

        var workspace: Workspace? = null

        val thrownBy = assertThatCode {
            workspace = runBlocking {
                workspaceService.getAccessibleWorkspace(fryWorkspace.id!!, mode)
            }
        }

        exceptionAssertions(thrownBy)

        workspaceAssertions(assertThat(workspace))
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = fry()
        val farnsworth = farnsworth()
        val zoidberg = zoidberg()
        val roberto = roberto()
        val mafiaBot = mafiaBot()
        val fryWorkspace = workspace(owner = fry)
        val farnsworthWorkspace = workspace(owner = farnsworth)
        val validTokenForFryWorkspace = workspaceAccessToken(
            workspace = fryWorkspace,
            token = "valid",
            validTill = MOCK_TIME.plus(Duration.ofDays(100)),
            revoked = false
        )
        val validTokenForFarnsworthWorkspace = workspaceAccessToken(
            workspace = farnsworthWorkspace,
            token = "validForFarnsworth",
            validTill = MOCK_TIME.plus(Duration.ofDays(100)),
            revoked = false
        )
        val expiredTokenForFryWorkspace = workspaceAccessToken(
            workspace = fryWorkspace,
            token = "expired",
            validTill = MOCK_TIME.minusMillis(1),
            revoked = false
        )
        val revokedTokenForFryWorkspace = workspaceAccessToken(
            workspace = fryWorkspace,
            token = "revoked",
            validTill = MOCK_TIME.plus(Duration.ofDays(100)),
            revoked = true
        )

        init {
            save(
                SavedWorkspaceAccessToken(
                    ownerId = farnsworth.id!!,
                    workspaceAccessTokenId = validTokenForFryWorkspace.id!!
                )
            )
            save(
                SavedWorkspaceAccessToken(
                    ownerId = roberto.id!!,
                    workspaceAccessTokenId = expiredTokenForFryWorkspace.id!!
                )
            )
            save(
                SavedWorkspaceAccessToken(
                    ownerId = mafiaBot.id!!,
                    workspaceAccessTokenId = revokedTokenForFryWorkspace.id!!
                )
            )
        }
    }
}
