package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("persistent_oauth2_authorized_client")
data class PersistentOAuth2AuthorizedClient(
    val clientRegistrationId: String,
    val userName: String,
    val accessToken: String,
    val accessTokenIssuedAt: Instant?,
    val accessTokenExpiresAt: Instant?,

    @field:MappedCollection(idColumn = "client_id")
    val accessTokenScopes: Set<ClientTokenScope>,

    val refreshToken: String?,
    val refreshTokenIssuedAt: Instant?,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,

) : AbstractEntity()

@Table("persistent_oauth2_authorized_client_access_token_scopes")
data class ClientTokenScope(
    @field:Column("access_token_scopes")
    val scope: String
)
