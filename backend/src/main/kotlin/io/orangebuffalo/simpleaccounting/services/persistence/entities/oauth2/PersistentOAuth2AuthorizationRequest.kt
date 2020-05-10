package io.orangebuffalo.simpleaccounting.services.persistence.entities.oauth2

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("PERSISTENT_OAUTH2_AUTHORIZATION_REQUEST")
class PersistentOAuth2AuthorizationRequest(
    val ownerId: Long,
    val state: String,
    val clientRegistrationId: String,
    val createWhen: Instant
) : AbstractEntity()
