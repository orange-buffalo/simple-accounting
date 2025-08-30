package io.orangebuffalo.simpleaccounting.business.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component

/**
 * A namespace for the user profile GraphQL API.
 */
class UserProfileApi {

    @GraphQLDescription("Provides access to the current user profile information.")
    @Component
    class UserProfileQuery(
        private val platformUsersService: PlatformUsersService,
    ) : Query {
        @Suppress("unused")
        @GraphQLDescription(
            "Returns the current user profile information. " +
                    "Current is defined as the user that is authenticated in the current request."
        )
        suspend fun userProfile(): UserProfile {
            return platformUsersService
                .getCurrentUser()
                .mapToProfileDto()
        }
    }

    @GraphQLDescription("Information about the user profile.")
    data class UserProfile(
        @param:GraphQLDescription("The user name / login of the user.")
        val userName: String,
        @param:GraphQLDescription("The identifier of the documents storage used by the user.")
        val documentsStorage: String?,
        @param:GraphQLDescription("Internationalization settings of the user.")
        val i18n: I18nSettings
    )

    @GraphQLDescription("Internationalization settings of the user profile.")
    data class I18nSettings(
        @param:GraphQLDescription(
            "The locale of the user profile, e.g. 'en-US'. " +
                    "Used for formatting dates, numbers, etc."
        )
        @field:Size(max = 36) val locale: String,
        @param:GraphQLDescription("The language of the user profile, e.g. 'en'. Used for translations.")
        @field:Size(max = 36) val language: String
    )
}

private fun PlatformUser.mapToProfileDto() = UserProfileApi.UserProfile(
    userName = userName,
    documentsStorage = documentsStorage,
    i18n = UserProfileApi.I18nSettings(
        locale = i18nSettings.locale,
        language = i18nSettings.language
    )
)
