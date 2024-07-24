package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.services.integration.TokenGenerator
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class WorkspaceAccessTokensService(
    private val repository: WorkspaceAccessTokensRepository,
    private val timeService: TimeService,
    private val tokenGenerator: TokenGenerator
) {

    suspend fun createAccessToken(workspace: Workspace, validTill: Instant): WorkspaceAccessToken = withDbContext {
        val token = WorkspaceAccessToken(
            workspaceId = workspace.id!!,
            validTill = validTill,
            revoked = false,
            timeCreated = timeService.currentTime(),
            token = tokenGenerator.generateToken()
        )
        repository.save(token)
    }

    suspend fun getValidToken(token: String): WorkspaceAccessToken? = withDbContext {
        repository.findValidByToken(token)
    }
}
