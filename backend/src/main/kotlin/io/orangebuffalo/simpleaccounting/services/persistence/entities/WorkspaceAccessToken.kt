package io.orangebuffalo.simpleaccounting.services.persistence.entities

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
