package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.EndpointUrl
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.oauthproviders.InvalidOAuthProviderScopeException
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvider
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProviderCreationException
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvidersService
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CreateOAuthProviderMutation(
    private val providersService: OAuthProvidersService,
) : Mutation {

    @Suppress("unused", "LongParameterList")
    @GraphQLDescription("Registers a new OAuth2 provider the users can authenticate with.")
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    @BusinessError(
        exceptionClass = OAuthProviderCreationException.ProviderAlreadyExistsException::class,
        errorCode = "PROVIDER_ALREADY_EXISTS",
        errorCodeDescription = "A provider with the given name already exists, ignoring case.",
    )
    @BusinessError(
        exceptionClass = InvalidOAuthProviderScopeException::class,
        errorCode = "INVALID_SCOPE",
        errorCodeDescription = "A scope is blank, too long, contains whitespace, or is duplicated.",
    )
    fun createOAuthProvider(
        @GraphQLDescription("Name of the provider, as presented to the users on the login page.")
        @NotBlank @Size(max = 255) name: String,
        @GraphQLDescription("Client ID issued by the provider for this application.")
        @NotBlank @Size(max = 255) clientId: String,
        @GraphQLDescription("Client secret issued by the provider for this application.")
        @NotBlank @Size(max = 255) clientSecret: String,
        @GraphQLDescription("Endpoint the users are redirected to in order to grant access to their identity.")
        @NotBlank @Size(max = 2048) @EndpointUrl authorizationUrl: String,
        @GraphQLDescription("Endpoint used to exchange the authorization code for an access token.")
        @NotBlank @Size(max = 2048) @EndpointUrl tokenUrl: String,
        @GraphQLDescription("Endpoint used to retrieve the details of the authenticated identity.")
        @NotBlank @Size(max = 2048) @EndpointUrl userInfoUrl: String,
        @GraphQLDescription("Name of the attribute in the user info response that uniquely identifies the user.")
        @NotBlank @Size(max = 255) userIdAttribute: String,
        @GraphQLDescription("Scopes to request from the provider. Blank and duplicate values are rejected.")
        scopes: List<String>,
    ): OAuthProviderGqlDto = providersService.createProvider(
        OAuthProvider(
            name = name,
            clientId = clientId,
            clientSecret = clientSecret,
            authorizationUrl = authorizationUrl,
            tokenUrl = tokenUrl,
            userInfoUrl = userInfoUrl,
            userIdAttribute = userIdAttribute,
            scopes = scopes.toProviderScopes(),
        )
    ).toOAuthProviderGqlDto()
}
