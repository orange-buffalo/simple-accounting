package io.orangebuffalo.simpleaccounting.business.api.profile

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import org.springframework.stereotype.Component

@Component
class UserProfileQuery(
    private val platformUsersService: PlatformUsersService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription(
        "Returns the current user profile information. " +
                "Current is defined as the user that is authenticated in the current request."
    )
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    suspend fun userProfile(): UserProfile {
        return platformUsersService
            .getCurrentUser()
            .mapToProfileDto()
    }

    private fun PlatformUser.mapToProfileDto() = UserProfile(
        userName = userName,
        documentsStorage = documentsStorage,
        i18n = I18nSettings(
            locale = i18nSettings.locale,
            language = i18nSettings.language
        )
    )
}
