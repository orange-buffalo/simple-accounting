package io.orangebuffalo.simpleaccounting.domain.workspaces

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
class WorkspaceAccessToken(
    var workspaceId: Long,
    val timeCreated: Instant,
    val validTill: Instant,
    val revoked: Boolean,
    val token: String
) : AbstractEntity()
