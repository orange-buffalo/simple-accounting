package io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("PERSISTENT_OAUTH2_AUTHORIZED_CLIENT")
class PersistentOAuth2AuthorizedClient(
    val clientRegistrationId: String,
    val userName: String,
    var accessToken: String,
    var accessTokenIssuedAt: Instant?,
    var accessTokenExpiresAt: Instant?,

    @field:MappedCollection(idColumn = "CLIENT_ID")
    var accessTokenScopes: Set<ClientTokenScope>,

    var refreshToken: String?,
    var refreshTokenIssuedAt: Instant?

) : AbstractEntity()

@Table("PERSISTENT_OAUTH2_AUTHORIZED_CLIENT_ACCESS_TOKEN_SCOPES")
data class ClientTokenScope(
    @field:Column("ACCESS_TOKEN_SCOPES")
    val scope: String
)
