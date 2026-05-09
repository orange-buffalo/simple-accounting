package io.orangebuffalo.simpleaccounting.business.security.remeberme

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
class RefreshToken(
    val userId: String,
    val token: String,
    var expirationTime: Instant
) : AbstractEntity()
