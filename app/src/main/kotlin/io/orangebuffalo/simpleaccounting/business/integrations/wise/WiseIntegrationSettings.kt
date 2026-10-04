package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
data class WiseIntegrationSettings(
    val workspaceId: String,
    val token: String,
    @field:MappedCollection(idColumn = "settings_id")
    val accounts: Set<WiseIntegrationAccount>,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()

@Table
data class WiseIntegrationAccount(
    val profileId: Long,
    val accountId: Long,
    val currency: String,
)
