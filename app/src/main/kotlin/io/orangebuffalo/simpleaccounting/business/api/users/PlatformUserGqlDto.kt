package io.orangebuffalo.simpleaccounting.business.api.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser

@GraphQLName("PlatformUser")
@GraphQLDescription("A platform user.")
data class PlatformUserGqlDto(
    @GraphQLDescription("The unique ID of the user.")
    val id: Long,
    @GraphQLDescription("The username / login of the user.")
    val userName: String,
    @GraphQLDescription("Whether the user has admin privileges.")
    val admin: Boolean,
    @GraphQLDescription("Whether the user account has been activated.")
    val activated: Boolean,
)

fun PlatformUser.toPlatformUserGqlDto() = PlatformUserGqlDto(
    id = id!!,
    userName = userName,
    admin = isAdmin,
    activated = activated,
)
