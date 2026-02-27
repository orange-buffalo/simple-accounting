package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class UpdateUserProfileMutation(
    private val platformUsersService: PlatformUsersService,
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription("Updates the current user profile information.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    suspend fun updateProfile(
        @GraphQLDescription("The identifier of the documents storage used by the user.")
        @Size(max = 255)
        documentsStorage: String?,
        @GraphQLDescription("The locale of the user profile, e.g. 'en-US'.")
        @Size(max = 36)
        locale: String,
        @GraphQLDescription("The language of the user profile, e.g. 'en'.")
        @Size(max = 36)
        language: String,
    ): UserProfileQuery.UserProfile {
        val user = platformUsersService.getCurrentUser()
        user.documentsStorage = documentsStorage
        user.i18nSettings.language = language
        user.i18nSettings.locale = locale
        val savedUser = platformUsersService.save(user)
        return UserProfileQuery.UserProfile(
            userName = savedUser.userName,
            documentsStorage = savedUser.documentsStorage,
            i18n = UserProfileQuery.I18nSettings(
                locale = savedUser.i18nSettings.locale,
                language = savedUser.i18nSettings.language,
            )
        )
    }
}
