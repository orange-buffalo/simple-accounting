package io.orangebuffalo.accounting.simpleaccounting.services.business

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.orangebuffalo.accounting.simpleaccounting.*
import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.services.integration.CoroutineAuthentication
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("WorkspaceService ")
internal class WorkspaceServiceIT(
    @Autowired val workspaceService: WorkspaceService
) {

    @MockBean
    private lateinit var timeService: TimeService

    @BeforeEach
    fun setup() {
        mockCurrentTime(timeService)
    }

    @Test
    fun `should fail to provide admin workspace access if user is not authenticated`(
        testData: WorkspaceServiceTestData
    ) {
        assertThatThrownBy {
            runBlocking {
                workspaceService.getAccessibleWorkspace(testData.fryWorkspace.id!!, WorkspaceAccessMode.ADMIN)
            }
        }.hasMessage("Authentication is not set")
    }

    @Test
    fun `should fail to provide read-write workspace access if user is not authenticated`(
        testData: WorkspaceServiceTestData
    ) {
        assertThatThrownBy {
            runBlocking {
                workspaceService.getAccessibleWorkspace(testData.fryWorkspace.id!!, WorkspaceAccessMode.READ_WRITE)
            }
        }.hasMessage("Authentication is not set")
    }

    @Test
    fun `should fail to provide read-only workspace access if user is not authenticated`(
        testData: WorkspaceServiceTestData
    ) {
        assertThatThrownBy {
            runBlocking {
                workspaceService.getAccessibleWorkspace(testData.fryWorkspace.id!!, WorkspaceAccessMode.READ_ONLY)
            }
        }.hasMessage("Authentication is not set")
    }

    @WithMockFryUser
    @Test
    fun `should provide admin access to workspace for its owner`(testData: WorkspaceServiceTestData) {
        assertSuccessfulGetAccessibleWorkspace(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockFarnsworthUser
    @Test
    fun `should not provide admin access to workspace for users with valid token`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockRobertoUser
    @Test
    fun `should not provide admin access to workspace for users with expired tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockMafiaBotUser
    @Test
    fun `should not provide admin access to workspace for users with revoked tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockZoidbergUser
    @Test
    fun `should not provide admin access to workspace for other users`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockFryUser
    @Test
    fun `should provide read-write access to workspace for its owner`(testData: WorkspaceServiceTestData) {
        assertSuccessfulGetAccessibleWorkspace(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockFarnsworthUser
    @Test
    fun `should not provide read-write access to workspace for users with valid token`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockRobertoUser
    @Test
    fun `should not provide read-write access to workspace for users with expired tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockMafiaBotUser
    @Test
    fun `should not provide read-write access to workspace for users with revoked tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockZoidbergUser
    @Test
    fun `should not provide read-write access to workspace for other users`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockFryUser
    @Test
    fun `should provide read-only access to workspace for its owner`(testData: WorkspaceServiceTestData) {
        assertSuccessfulGetAccessibleWorkspace(testData, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockFarnsworthUser
    @Test
    fun `should provide read-only access to workspace for users with valid token`(testData: WorkspaceServiceTestData) {
        assertSuccessfulGetAccessibleWorkspace(testData, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockRobertoUser
    @Test
    fun `should not provide read-only access to workspace for users with expired tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockMafiaBotUser
    @Test
    fun `should not provide read-only access to workspace for users with revoked tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockZoidbergUser
    @Test
    fun `should not provide read-only access to workspace for other users`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_ONLY)
    }

    private fun assertThatGetAccessibleWorkspaceFailed(
        testData: WorkspaceServiceTestData,
        mode: WorkspaceAccessMode
    ) {
        assertThatGetAccessibleWorkspace(
            testData,
            mode, {
                isInstanceOf(EntityNotFoundException::class.java)
                    .hasMessage("Workspace ${testData.fryWorkspace.id} is not found")
            },
            {})
    }

    private fun assertSuccessfulGetAccessibleWorkspace(
        testData: WorkspaceServiceTestData,
        mode: WorkspaceAccessMode
    ) {
        assertThatGetAccessibleWorkspace(
            testData,
            mode,
            { doesNotThrowAnyException() },
            { isEqualTo(testData.fryWorkspace) })
    }

    private fun assertThatGetAccessibleWorkspace(
        testData: WorkspaceServiceTestData,
        mode: WorkspaceAccessMode,
        exceptionAssertions: AbstractThrowableAssert<*, *>.() -> Unit,
        workspaceAssertions: Assert<Workspace?>.() -> Unit
    ) {

        var workspace: Workspace? = null

        val thrownBy = assertThatCode {
            workspace = runBlocking(CoroutineAuthentication(SecurityContextHolder.getContext().authentication)) {
                workspaceService.getAccessibleWorkspace(testData.fryWorkspace.id!!, mode)
            }
        }

        exceptionAssertions(thrownBy)

        workspaceAssertions(assertThat(workspace))
    }

    class WorkspaceServiceTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val zoidberg = Prototypes.zoidberg()
        val roberto = Prototypes.roberto()
        val mafiaBot = Prototypes.mafiaBot()
        val fryWorkspace = Prototypes.workspace(owner = fry)
        val validTokenForFryWorkspace = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            token = "valid",
            validTill = MOCK_TIME.plus(Duration.ofDays(100)),
            revoked = false
        )
        val expiredTokenForFryWorkspace = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            token = "valid",
            validTill = MOCK_TIME.minusMillis(1),
            revoked = false
        )
        val revokedTokenForFryWorkspace = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            token = "valid",
            validTill = MOCK_TIME.plus(Duration.ofDays(100)),
            revoked = true
        )

        override fun generateData() = listOf(
            fry, farnsworth, zoidberg, roberto, mafiaBot,
            fryWorkspace,
            validTokenForFryWorkspace,
            SavedWorkspaceAccessToken(owner = farnsworth, workspaceAccessToken = validTokenForFryWorkspace),
            expiredTokenForFryWorkspace,
            SavedWorkspaceAccessToken(owner = roberto, workspaceAccessToken = expiredTokenForFryWorkspace),
            revokedTokenForFryWorkspace,
            SavedWorkspaceAccessToken(owner = mafiaBot, workspaceAccessToken = revokedTokenForFryWorkspace)
        )
    }
}
