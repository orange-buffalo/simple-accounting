package io.orangebuffalo.simpleaccounting.business.api.auth

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationService
import io.orangebuffalo.simpleaccounting.business.oauthproviders.UserAuthenticationMethod
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class UserAuthenticationMethodsQuery(
    private val oauthUserAuthenticationService: OAuthUserAuthenticationService,
) : Query {

    @Suppress("unused")
    @GraphQLDescription(
        "Returns the authentication methods available for the user with the provided name. " +
                "This is the first step of the login process. " +
                "For users that have linked an identity at an OAuth2 provider, password login is not offered. " +
                "For unknown users, password login is reported, so that account existence is not disclosed."
    )
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    fun userAuthenticationMethods(
        @GraphQLDescription("The username of the user attempting to login.")
        @NotBlank
        userName: String,
    ): List<UserAuthenticationMethodGqlDto> = oauthUserAuthenticationService
        .getAuthenticationMethods(userName)
        .map { method ->
            when (method) {
                is UserAuthenticationMethod.PasswordMethod -> UserAuthenticationMethodGqlDto(
                    type = AuthenticationMethodType.PASSWORD,
                    providerId = null,
                    providerName = null,
                )

                is UserAuthenticationMethod.OAuthMethod -> UserAuthenticationMethodGqlDto(
                    type = AuthenticationMethodType.OAUTH,
                    providerId = method.providerId,
                    providerName = method.providerName,
                )
            }
        }

    @GraphQLDescription("A way for a particular user to authenticate with the application.")
    data class UserAuthenticationMethodGqlDto(
        @GraphQLDescription("The type of the authentication method.")
        val type: AuthenticationMethodType,
        @GraphQLDescription("ID of the OAuth2 provider. Only provided for the OAUTH method type.")
        val providerId: String?,
        @GraphQLDescription("Name of the OAuth2 provider. Only provided for the OAUTH method type.")
        val providerName: String?,
    )

    @GraphQLDescription("Type of the authentication method available for a user.")
    enum class AuthenticationMethodType {
        @GraphQLDescription("The user authenticates with their username and password.")
        PASSWORD,

        @GraphQLDescription("The user authenticates with an identity linked at an OAuth2 provider.")
        OAUTH,
    }
}
