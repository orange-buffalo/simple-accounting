package io.orangebuffalo.simpleaccounting.domain.workspaces

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.relational.core.mapping.Table

@Table
class SavedWorkspaceAccessToken(
    var workspaceAccessTokenId: Long,
    var ownerId: Long
) : AbstractEntity()
