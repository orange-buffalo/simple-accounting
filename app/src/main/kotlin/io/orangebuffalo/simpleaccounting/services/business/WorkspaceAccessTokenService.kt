package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.TokenGenerator
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.repos.WorkspaceAccessTokenRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class WorkspaceAccessTokenService(
    private val repository: WorkspaceAccessTokenRepository,
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
