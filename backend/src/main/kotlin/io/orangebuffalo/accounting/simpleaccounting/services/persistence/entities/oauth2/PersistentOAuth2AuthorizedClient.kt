package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.oauth2

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.AbstractEntity
import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "persistent_oauth2_authorized_client")
class PersistentOAuth2AuthorizedClient(

    @field:Column(nullable = false)
    val clientRegistrationId: String,

    @field:Column(nullable = false)
    val userName: String,

    @field:Column(nullable = false)
    var accessToken: String,

    @field:Column
    var accessTokenIssuedAt: Instant?,

    @field:Column
    var accessTokenExpiresAt: Instant?,

    @field:ElementCollection(fetch = FetchType.EAGER)
    @field:CollectionTable(
        name = "persistent_oauth2_authorized_client_access_token_scopes",
        joinColumns = [JoinColumn(
            name = "client_id",
            foreignKey = ForeignKey(name = "pauth2ac_access_token_scopes_scopes_client_fk")
        )]
    )
    var accessTokenScopes: Set<String>,

    @field:Column
    var refreshToken: String?,

    @field:Column
    var refreshTokenIssuedAt: Instant?

) : AbstractEntity()
