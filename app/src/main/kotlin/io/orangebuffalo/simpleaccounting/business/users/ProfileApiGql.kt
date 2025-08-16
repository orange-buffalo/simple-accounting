package io.orangebuffalo.simpleaccounting.business.users

import com.expediagroup.graphql.server.operations.Query
import org.springframework.stereotype.Component

@Component
class UserProfileQuery(
    private val platformUsersService: PlatformUsersService,
) : Query {
    suspend fun userProfile(): UserProfileDto = platformUsersService
        .getCurrentUser()
        .mapToProfileDto()

    data class UserProfileDto(
        val userName: String,
        val documentsStorage: String?,
        val i18n: I18nSettingsDto
    )
}

private fun PlatformUser.mapToProfileDto() = UserProfileQuery.UserProfileDto(
    userName = userName,
    documentsStorage = documentsStorage,
    i18n = I18nSettingsDto(
        locale = i18nSettings.locale,
        language = i18nSettings.language
    )
)
