package io.orangebuffalo.simpleaccounting.business.users

import com.expediagroup.graphql.server.operations.Query
import org.springframework.stereotype.Component

data class UserProfile(
    val test: String,
)

@Component
class MyProfileQuery(
    private val platformUsersService: PlatformUsersService,
) : Query {
    suspend fun myProfile(): MyProfileDto = platformUsersService
        .getCurrentUser()
        .mapToProfileDto()

    data class MyProfileDto(
        val userName: String,
        val documentsStorage: String?,
        val i18n: I18nSettingsDto
    )
}

private fun PlatformUser.mapToProfileDto() = MyProfileQuery.MyProfileDto(
    userName = userName,
    documentsStorage = documentsStorage,
    i18n = I18nSettingsDto(
        locale = i18nSettings.locale,
        language = i18nSettings.language
    )
)
