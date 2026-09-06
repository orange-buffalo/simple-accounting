package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.api.directives.EndpointUrl
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OidcProviderConfiguration
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OidcProviderDiscoveryException
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OidcProviderDiscoveryService
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class DiscoverOidcProviderConfigurationQuery(
    private val discoveryService: OidcProviderDiscoveryService,
) : Query {

    @Suppress("unused")
    @GraphQLDescription("Loads OAuth2 endpoints from an OpenID Connect discovery document.")
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    @BusinessError(
        exceptionClass = OidcProviderDiscoveryException::class,
        errorCode = "DISCOVERY_FAILED",
        errorCodeDescription = "A compatible OpenID Connect configuration could not be loaded from the base URL.",
    )
    fun discoverOidcProviderConfiguration(
        @GraphQLDescription("Base URL of the OpenID Connect provider.")
        @NotBlank @Size(max = 2048) @EndpointUrl baseUrl: String,
    ): OidcProviderConfigurationGqlDto = discoveryService.discover(baseUrl).toGqlDto()
}

@GraphQLName("OidcProviderConfiguration")
data class OidcProviderConfigurationGqlDto(
    val authorizationUrl: String,
    val tokenUrl: String,
    val userInfoUrl: String,
    val userIdAttribute: String,
    val scopes: List<String>,
)

private fun OidcProviderConfiguration.toGqlDto() = OidcProviderConfigurationGqlDto(
    authorizationUrl = authorizationUrl,
    tokenUrl = tokenUrl,
    userInfoUrl = userInfoUrl,
    userIdAttribute = userIdAttribute,
    scopes = scopes,
)
