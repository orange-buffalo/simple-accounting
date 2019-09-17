package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.data.repository.query.Param
import java.time.Instant

interface WorkspaceAccessTokenRepository : AbstractEntityRepository<WorkspaceAccessToken>,
    QuerydslPredicateExecutor<WorkspaceAccessToken> {

    @Query(
        "from WorkspaceAccessToken t " +
                "where t.token = :token " +
                "and t.validTill > :currentTime"
    )
    fun findValidByToken(
        @Param("token") token: String,
        @Param("currentTime") currentTime: Instant
    ): WorkspaceAccessToken?
}