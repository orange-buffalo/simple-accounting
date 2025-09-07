package io.orangebuffalo.simpleaccounting.business.security.authentication

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import io.orangebuffalo.simpleaccounting.infra.graphql.RequiredAuth
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component

/**
 * A namespace for the authentication GraphQL API.
 */
@Suppress("unused")
class AuthenticationGqlApi {

    @GraphQLDescription("Provides access to authentication operations.")
    @Component
    class AuthenticationMutation(
        private val jwtService: JwtService,
        private val workspaceAccessTokensService: WorkspaceAccessTokensService
    ) : Mutation {

        @Suppress("unused")
        @GraphQLDescription(
            "Refreshes the access token using the refresh token from cookies or current authentication. " +
                    "Returns a response with either a valid access token or null if authentication fails."
        )
        @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
        suspend fun refreshAccessToken(): RefreshAccessTokenResponse {
            val currentAuth = ReactiveSecurityContextHolder.getContext()
                .map { it.authentication }
                .awaitFirstOrNull()

            val authenticatedAuth = if (currentAuth != null && currentAuth.isAuthenticated) {
                currentAuth
            } else {
                return RefreshAccessTokenResponse(accessToken = null)
            }

            val principal = authenticatedAuth.principal as SecurityPrincipal
            val jwtToken = if (principal.isTransient) {
                val workspaceAccessToken = workspaceAccessTokensService.getValidToken(principal.userName)
                if (workspaceAccessToken != null) {
                    jwtService.buildJwtToken(principal, workspaceAccessToken.validTill)
                } else {
                    null
                }
            } else {
                jwtService.buildJwtToken(principal)
            }
            return RefreshAccessTokenResponse(accessToken = jwtToken)
        }
    }

    @GraphQLDescription("Response for refreshing access token.")
    data class RefreshAccessTokenResponse(
        @param:GraphQLDescription(
            "The new access token if authentication was successful, null otherwise."
        )
        val accessToken: String?
    )
}
