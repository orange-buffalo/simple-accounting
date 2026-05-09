package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("PERSISTENT_OAUTH2_AUTHORIZED_CLIENT")
data class PersistentOAuth2AuthorizedClient(
    val clientRegistrationId: String,
    val userName: String,
    val accessToken: String,
    val accessTokenIssuedAt: Instant?,
    val accessTokenExpiresAt: Instant?,

    @field:MappedCollection(idColumn = "CLIENT_ID")
    val accessTokenScopes: Set<ClientTokenScope>,

    val refreshToken: String?,
    val refreshTokenIssuedAt: Instant?,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,

) : AbstractEntity()

@Table("PERSISTENT_OAUTH2_AUTHORIZED_CLIENT_ACCESS_TOKEN_SCOPES")
data class ClientTokenScope(
    @field:Column("ACCESS_TOKEN_SCOPES")
    val scope: String
)
