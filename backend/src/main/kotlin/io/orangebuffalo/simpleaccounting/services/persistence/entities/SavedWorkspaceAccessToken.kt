package io.orangebuffalo.simpleaccounting.services.persistence.entities

import org.springframework.data.relational.core.mapping.Table

@Table
class SavedWorkspaceAccessToken(
    var workspaceAccessTokenId: Long,
    var ownerId: Long
) : AbstractEntity()
