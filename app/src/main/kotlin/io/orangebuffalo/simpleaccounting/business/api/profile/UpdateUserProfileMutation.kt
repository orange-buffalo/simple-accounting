package io.orangebuffalo.simpleaccounting.business.api.profile

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import jakarta.validation.constraints.NotBlank
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
    fun updateProfile(
        @GraphQLDescription("The identifier of the documents storage used by the user.")
        @Size(max = 255)
        documentsStorage: String? = null,
        @GraphQLDescription("The locale of the user profile, e.g. 'en-US'.")
        @NotBlank
        @Size(max = 36)
        locale: String,
        @GraphQLDescription("The language of the user profile, e.g. 'en'.")
        @NotBlank
        @Size(max = 36)
        language: String,
        @GraphQLDescription("Required IANA time zone, e.g. 'Australia/Melbourne'. Null, empty, blank and unknown zones are rejected.")
        @ValidTimeZone
        timeZone: String,
    ): UserProfile {
        val user = platformUsersService.getCurrentUser()
        val savedUser = platformUsersService.save(
            user.copy(
                documentsStorage = documentsStorage,
                i18nSettings = user.i18nSettings.copy(
                    locale = locale,
                    language = language,
                    timeZone = timeZone,
                )
            )
        )
        return UserProfile(
            userName = savedUser.userName,
            documentsStorage = savedUser.documentsStorage,
            i18n = I18nSettings(
                locale = savedUser.i18nSettings.locale,
                language = savedUser.i18nSettings.language,
                timeZone = savedUser.i18nSettings.timeZone,
            )
        )
    }
}
