package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.oauth2

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.AbstractEntity
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "persistent_oauth2_authorization_request")
class PersistentOAuth2AuthorizationRequest(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(
        nullable = false,
        foreignKey = ForeignKey(name = "persistent_oauth2_authorization_request_owner_fk")
    )
    val owner: PlatformUser,

    @field:Column(nullable = false, length = 512)
    val state: String,

    @field:Column(nullable = false)
    val clientRegistrationId: String,

    @field:Column(nullable = false)
    val createWhen: Instant

) : AbstractEntity()