package io.orangebuffalo.simpleaccounting.services.persistence.entities

import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
class RefreshToken(
    val userId: Long,
    val token: String,
    var expirationTime: Instant
) : AbstractEntity()
