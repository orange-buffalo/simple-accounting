package io.orangebuffalo.accounting.simpleaccounting.services.business

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.orangebuffalo.accounting.simpleaccounting.MOCK_TIME
import io.orangebuffalo.accounting.simpleaccounting.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.mockCurrentTime
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
import org.springframework.security.test.context.support.WithMockUser
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

    @WithMockUser(roles = ["USER"], username = "Fry")
    @Test
    fun `should provide admin access to workspace for its owner`(testData: WorkspaceServiceTestData) {
        assertSuccessfulGetAccessibleWorkspace(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    @Test
    fun `should not provide admin access to workspace for users with valid token`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockUser(roles = ["USER"], username = "Roberto")
    @Test
    fun `should not provide admin access to workspace for users with expired tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockUser(roles = ["USER"], username = "MafiaBot")
    @Test
    fun `should not provide admin access to workspace for users with revoked tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockUser(roles = ["USER"], username = "Zoidberg")
    @Test
    fun `should not provide admin access to workspace for other users`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.ADMIN)
    }

    @WithMockUser(roles = ["USER"], username = "Fry")
    @Test
    fun `should provide read-write access to workspace for its owner`(testData: WorkspaceServiceTestData) {
        assertSuccessfulGetAccessibleWorkspace(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    @Test
    fun `should not provide read-write access to workspace for users with valid token`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockUser(roles = ["USER"], username = "Roberto")
    @Test
    fun `should not provide read-write access to workspace for users with expired tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockUser(roles = ["USER"], username = "MafiaBot")
    @Test
    fun `should not provide read-write access to workspace for users with revoked tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockUser(roles = ["USER"], username = "Zoidberg")
    @Test
    fun `should not provide read-write access to workspace for other users`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_WRITE)
    }

    @WithMockUser(roles = ["USER"], username = "Fry")
    @Test
    fun `should provide read-only access to workspace for its owner`(testData: WorkspaceServiceTestData) {
        assertSuccessfulGetAccessibleWorkspace(testData, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    @Test
    fun `should provide read-only access to workspace for users with valid token`(testData: WorkspaceServiceTestData) {
        assertSuccessfulGetAccessibleWorkspace(testData, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockUser(roles = ["USER"], username = "Roberto")
    @Test
    fun `should not provide read-only access to workspace for users with expired tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockUser(roles = ["USER"], username = "MafiaBot")
    @Test
    fun `should not provide read-only access to workspace for users with revoked tokens`(testData: WorkspaceServiceTestData) {
        assertThatGetAccessibleWorkspaceFailed(testData, WorkspaceAccessMode.READ_ONLY)
    }

    @WithMockUser(roles = ["USER"], username = "Zoidberg")
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
        val roberto = Prototypes.platformUser(userName = "Roberto")
        val mafiaBot = Prototypes.platformUser(userName = "MafiaBot")
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
