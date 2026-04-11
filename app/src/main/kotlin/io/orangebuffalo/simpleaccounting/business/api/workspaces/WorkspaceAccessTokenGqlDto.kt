package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessToken
import java.time.Instant

@GraphQLName("WorkspaceAccessToken")
@GraphQLDescription("An access token for sharing workspace access.")
data class WorkspaceAccessTokenGqlDto(
    @GraphQLDescription("ID of the access token.")
    val id: Long,

    @GraphQLDescription("Version of the access token.")
    val version: Int,

    @GraphQLDescription("The expiration time of this token.")
    val validTill: Instant,

    @GraphQLDescription("Whether this token has been revoked.")
    val revoked: Boolean,

    @GraphQLDescription("The token value used to share workspace access.")
    val token: String,
)

internal fun WorkspaceAccessToken.toWorkspaceAccessTokenGqlDto() = WorkspaceAccessTokenGqlDto(
    id = id!!,
    version = version!!,
    validTill = validTill,
    revoked = revoked,
    token = token,
)
