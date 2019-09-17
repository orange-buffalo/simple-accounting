package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SavedWorkspaceAccessTokenRepository : AbstractEntityRepository<SavedWorkspaceAccessToken> {

    fun findByWorkspaceAccessTokenAndOwner(
        workspaceAccessToken: WorkspaceAccessToken,
        owner: PlatformUser
    ): SavedWorkspaceAccessToken?

    @Query(
        "from SavedWorkspaceAccessToken savedToken " +
                "where savedToken.owner = :owner " +
                "and savedToken.workspaceAccessToken.validTill > :currentTime"
    )
    fun findAllValidByOwner(
        @Param("owner") owner: PlatformUser,
        @Param("currentTime") currentTime: Instant
    ): List<SavedWorkspaceAccessToken>
}