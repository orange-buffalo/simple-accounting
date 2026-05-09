package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
data class WorkspaceAccessToken(
    val workspaceId: String,
    val timeCreated: Instant,
    val validTill: Instant,
    val revoked: Boolean,
    val token: String,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
