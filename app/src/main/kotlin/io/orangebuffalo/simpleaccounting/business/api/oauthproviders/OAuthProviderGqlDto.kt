package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthProvider

@GraphQLName("OAuthProvider")
@GraphQLDescription("An OAuth2 provider the users can authenticate with.")
data class OAuthProviderGqlDto(
    @GraphQLDescription("The unique ID of the provider.")
    val id: String,
    @GraphQLDescription("Version of the provider state.")
    val version: Int,
    @GraphQLDescription("Name of the provider, as presented to the users on the login page.")
    val name: String,
    @GraphQLDescription("Client ID issued by the provider for this application.")
    val clientId: String,
    @GraphQLDescription("Endpoint the users are redirected to in order to grant access to their identity.")
    val authorizationUrl: String,
    @GraphQLDescription("Endpoint used to exchange the authorization code for an access token.")
    val tokenUrl: String,
    @GraphQLDescription("Endpoint used to retrieve the details of the authenticated identity.")
    val userInfoUrl: String,
    @GraphQLDescription(
        "Name of the attribute in the user info response that uniquely identifies the user " +
                "within the provider, e.g. 'sub' or 'email'."
    )
    val userIdAttribute: String,
    @GraphQLDescription("Scopes requested from the provider, sorted alphabetically.")
    val scopes: List<String>,
)

fun OAuthProvider.toOAuthProviderGqlDto() = OAuthProviderGqlDto(
    id = id!!,
    version = version!!,
    name = name,
    clientId = clientId,
    authorizationUrl = authorizationUrl,
    tokenUrl = tokenUrl,
    userInfoUrl = userInfoUrl,
    userIdAttribute = userIdAttribute,
    scopes = scopes.map { it.scope }.sorted(),
)
