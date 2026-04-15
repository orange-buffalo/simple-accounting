package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.users.UserActivationToken
import java.time.Instant

@GraphQLDescription("A user activation token used to activate a new user account.")
data class UserActivationTokenGqlDto(
    @GraphQLDescription("The token value.")
    val token: String,
    @GraphQLDescription("The date and time when the token expires.")
    val expiresAt: Instant,
)

fun UserActivationToken.mapToGqlDto() = UserActivationTokenGqlDto(
    token = token,
    expiresAt = expiresAt,
)
