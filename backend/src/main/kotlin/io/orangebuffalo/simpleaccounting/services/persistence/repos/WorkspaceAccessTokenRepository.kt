package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.data.repository.query.Param
import java.time.Instant

interface WorkspaceAccessTokenRepository : LegacyAbstractEntityRepository<WorkspaceAccessToken>,
    QuerydslPredicateExecutor<WorkspaceAccessToken> {

    @Query(
        """
            from WorkspaceAccessToken t 
            where 
                t.token = :token 
                and t.validTill > :currentTime
                and t.revoked = false 
        """
    )
    fun findValidByToken(
        @Param("token") token: String,
        @Param("currentTime") currentTime: Instant
    ): WorkspaceAccessToken?

    @Query(
        """
            from WorkspaceAccessToken t 
            where 
                t.token = :token 
                and t.validTill > :currentTime
                and t.revoked = false 
                and t.workspace.id = :workspace
        """
    )
    @EntityGraph(attributePaths = ["workspace"], type = FETCH)
    fun findValidByTokenAndWorkspaceWithFetchedWorkspace(
        @Param("token") token: String,
        @Param("currentTime") currentTime: Instant,
        @Param("workspace") workspace: Long
    ): WorkspaceAccessToken?
}
