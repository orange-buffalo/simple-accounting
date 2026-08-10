package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class WorkspaceAccessTokensService(
    private val repository: WorkspaceAccessTokensRepository,
    private val savedWorkspaceAccessTokensRepository: SavedWorkspaceAccessTokensRepository,
    private val timeService: TimeService,
    private val tokenGenerator: TokenGenerator
) {

    fun createAccessToken(workspace: Workspace, validTill: Instant): WorkspaceAccessToken {
        val token = WorkspaceAccessToken(
            workspaceId = workspace.id!!,
            validTill = validTill,
            revoked = false,
            timeCreated = timeService.currentTime(),
            token = tokenGenerator.generateToken()
        )
        return repository.save(token)
    }

    fun getValidToken(token: String): WorkspaceAccessToken? = repository.findValidByToken(token)

    fun getToken(accessTokenId: String): WorkspaceAccessToken =
        repository.findById(accessTokenId)
            .orElseThrow { EntityNotFoundException("Workspace access token $accessTokenId is not found") }

    fun deleteAccessToken(accessToken: WorkspaceAccessToken) {
        savedWorkspaceAccessTokensRepository.deleteByWorkspaceAccessTokenId(accessToken.id!!)
        repository.delete(accessToken)
    }
}
