package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class OAuthProvidersQuery(
    private val paginationService: GraphqlPaginationService,
    private val dslContext: DSLContext,
) : Query {

    @Suppress("unused")
    @GraphQLDescription(
        "Returns the registered OAuth2 providers with cursor-based pagination. " +
                "Only accessible by admin users."
    )
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    fun oauthProviders(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        @GraphQLDescription("Optional free-text search filter applied to the provider name.")
        freeSearchText: String? = null,
    ): ConnectionGqlDto<OAuthProviderGqlDto> {
        val provider = Tables.OAUTH_PROVIDER
        val providerScope = Tables.OAUTH_PROVIDER_SCOPE
        return paginationService.forTable(provider)
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(provider.name.containsIgnoreCase(freeSearchText))
                }
            }
            .page(
                first = first,
                after = after,
                mapQueryRecord = { record ->
                    OAuthProviderGqlDto(
                        id = record[provider.id]!!,
                        version = record[provider.version]!!,
                        name = record[provider.name]!!,
                        clientId = record[provider.clientId]!!,
                        authorizationUrl = record[provider.authorizationUrl]!!,
                        tokenUrl = record[provider.tokenUrl]!!,
                        userInfoUrl = record[provider.userInfoUrl]!!,
                        userIdAttribute = record[provider.userIdAttribute]!!,
                        scopes = emptyList(),
                    )
                },
                postProcess = { providers ->
                    val scopesByProviderId = dslContext
                        .select(providerScope.providerId, providerScope.scope)
                        .from(providerScope)
                        .where(providerScope.providerId.`in`(providers.map { it.id }))
                        .fetch()
                        .groupBy(
                            { it[providerScope.providerId]!! },
                            { it[providerScope.scope]!! },
                        )

                    providers.map { it.copy(scopes = scopesByProviderId[it.id]?.sorted() ?: emptyList()) }
                },
            )
    }
}
