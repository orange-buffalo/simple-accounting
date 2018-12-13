package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import java.time.Instant
import javax.persistence.*

@Entity
class RefreshToken(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "refresh_token_user_fk"))
    val user: PlatformUser,

    @field:Column(nullable = false, length = 2048)
    val token: String,

    @field:Column(nullable = false)
    var expirationTime: Instant

) : AbstractEntity()