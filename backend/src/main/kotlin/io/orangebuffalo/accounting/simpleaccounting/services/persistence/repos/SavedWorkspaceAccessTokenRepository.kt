package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SavedWorkspaceAccessTokenRepository : AbstractEntityRepository<SavedWorkspaceAccessToken> {

    fun findByWorkspaceAccessTokenAndOwner(
        workspaceAccessToken: WorkspaceAccessToken,
        owner: PlatformUser
    ): SavedWorkspaceAccessToken?

    @Query(
        """
            from SavedWorkspaceAccessToken savedToken 
            where 
                savedToken.owner.userName = :owner 
                and savedToken.workspaceAccessToken.validTill > :currentTime
                and savedToken.workspaceAccessToken.revoked = false
        """
    )
    fun findAllValidByOwner(
        @Param("owner") owner: String,
        @Param("currentTime") currentTime: Instant
    ): List<SavedWorkspaceAccessToken>

    @Query(
        """
            from SavedWorkspaceAccessToken savedToken 
            where 
                savedToken.owner.userName = :owner 
                and savedToken.workspaceAccessToken.validTill > :currentTime
                and savedToken.workspaceAccessToken.revoked = false 
                and savedToken.workspaceAccessToken.workspace.id = :workspace
        """
    )
    @EntityGraph(attributePaths = ["workspaceAccessToken.workspace"], type = FETCH)
    fun findValidByOwnerAndWorkspaceWithFetchedWorkspace(
        @Param("owner") owner: String,
        @Param("workspace") workspace: Long,
        @Param("currentTime") currentTime: Instant
    ): SavedWorkspaceAccessToken?
}