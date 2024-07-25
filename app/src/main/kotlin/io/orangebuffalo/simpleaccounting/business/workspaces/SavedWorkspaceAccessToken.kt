package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table

@Table
class SavedWorkspaceAccessToken(
    var workspaceAccessTokenId: Long,
    var ownerId: Long
) : AbstractEntity()
