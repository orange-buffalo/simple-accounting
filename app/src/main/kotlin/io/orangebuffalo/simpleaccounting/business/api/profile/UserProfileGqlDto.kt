package io.orangebuffalo.simpleaccounting.business.api.profile

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import jakarta.validation.constraints.Size

@GraphQLDescription("Information about the user profile.")
data class UserProfile(
    @GraphQLDescription("The user name / login of the user.")
    val userName: String,
    @GraphQLDescription("The identifier of the documents storage used by the user.")
    val documentsStorage: String?,
    @GraphQLDescription("Internationalization settings of the user.")
    val i18n: I18nSettings
)

@GraphQLDescription("Internationalization settings of the user profile.")
data class I18nSettings(
    @GraphQLDescription(
        "The locale of the user profile, e.g. 'en-US'. " +
                "Used for formatting dates, numbers, etc."
    )
    @field:Size(max = 36) val locale: String,
    @GraphQLDescription("The language of the user profile, e.g. 'en'. Used for translations.")
    @field:Size(max = 36) val language: String
)
