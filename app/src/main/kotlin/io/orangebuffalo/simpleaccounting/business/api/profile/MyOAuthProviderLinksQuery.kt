package io.orangebuffalo.simpleaccounting.business.api.profile

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvidersService
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationService
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import org.springframework.stereotype.Component

@Component
class MyOAuthProviderLinksQuery(
    private val providersService: OAuthProvidersService,
    private val oauthUserAuthenticationService: OAuthUserAuthenticationService,
    private val platformUsersService: PlatformUsersService,
) : Query {

    @Suppress("unused")
    @GraphQLDescription(
        "Returns all registered OAuth2 providers together with the identity the current user " +
                "has linked at each of them, if any. Sorted by provider name."
    )
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    fun myOAuthProviderLinks(): List<OAuthProviderLinkGqlDto> {
        val currentUser = platformUsersService.getCurrentUser()
        val identitiesByProviderId = oauthUserAuthenticationService.getLinkedIdentities(currentUser)
            .associateBy { it.providerId }
        return providersService.getProviders()
            .sortedBy { it.name }
            .map { provider ->
                OAuthProviderLinkGqlDto(
                    providerId = provider.id!!,
                    providerName = provider.name,
                    externalId = identitiesByProviderId[provider.id]?.externalId,
                )
            }
    }

    @GraphQLDescription("An OAuth2 provider and the identity the current user has linked at it.")
    data class OAuthProviderLinkGqlDto(
        @GraphQLDescription("ID of the provider.")
        val providerId: String,
        @GraphQLDescription("Name of the provider.")
        val providerName: String,
        @GraphQLDescription(
            "Identifier of the linked identity at this provider. " +
                    "Null when the current user has not linked an identity at this provider."
        )
        val externalId: String?,
    )
}
