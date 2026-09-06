package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvidersService
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import org.springframework.stereotype.Component

@Component
class OAuthProviderQuery(
    private val providersService: OAuthProvidersService,
) : Query {

    @Suppress("unused")
    @GraphQLDescription("Returns the OAuth2 provider with the given ID.")
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    fun oauthProvider(
        @GraphQLDescription("ID of the provider.") id: String,
    ): OAuthProviderGqlDto = providersService.getProviderById(id).toOAuthProviderGqlDto()
}
