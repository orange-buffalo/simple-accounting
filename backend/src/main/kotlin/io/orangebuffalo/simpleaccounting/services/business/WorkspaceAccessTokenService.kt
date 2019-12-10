package io.orangebuffalo.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.simpleaccounting.services.integration.TokenGenerator
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.QWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.repos.WorkspaceAccessTokenRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class WorkspaceAccessTokenService(
    private val repository: WorkspaceAccessTokenRepository,
    private val timeService: TimeService,
    private val tokenGenerator: TokenGenerator
) {

    suspend fun getAccessTokens(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<WorkspaceAccessToken> = withDbContext {
        repository.findAll(QWorkspaceAccessToken.workspaceAccessToken.workspace.eq(workspace).and(filter), page)
    }

    suspend fun createAccessToken(workspace: Workspace, validTill: Instant): WorkspaceAccessToken = withDbContext {
        val token = WorkspaceAccessToken(
            workspace = workspace,
            validTill = validTill,
            revoked = false,
            timeCreated = timeService.currentTime(),
            token = tokenGenerator.generateToken()
        )
        repository.save(token)
    }

    suspend fun getValidToken(token: String): WorkspaceAccessToken? =
        withDbContext {
            repository.findValidByToken(token, timeService.currentTime())
        }
}
