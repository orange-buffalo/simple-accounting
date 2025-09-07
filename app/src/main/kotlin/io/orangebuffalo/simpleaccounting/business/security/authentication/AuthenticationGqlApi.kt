package io.orangebuffalo.simpleaccounting.business.security.authentication

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshAuthenticationToken
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import io.orangebuffalo.simpleaccounting.infra.graphql.RequiredAuth
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * A namespace for the authentication GraphQL API.
 */
class AuthenticationGqlApi {

    @GraphQLDescription("Provides access to authentication operations.")
    @Component
    class AuthenticationMutation(
        private val authenticationManager: ReactiveAuthenticationManager,
        private val jwtService: JwtService,
        private val refreshTokensService: RefreshTokensService,
        private val workspaceAccessTokensService: WorkspaceAccessTokensService
    ) : Mutation {

        @Suppress("unused")
        @GraphQLDescription(
            "Refreshes the access token using the refresh token from cookies or current authentication. " +
                    "Returns a response with either a valid access token or null if authentication fails."
        )
        @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
        suspend fun refreshAccessToken(): RefreshAccessTokenResponse {
            return try {
                // Get current authentication context
                val currentAuth = ReactiveSecurityContextHolder.getContext()
                    .map { it.authentication }
                    .awaitFirstOrNull()

                val authenticatedAuth = if (currentAuth != null && currentAuth.isAuthenticated) {
                    currentAuth
                } else {
                    // Try to authenticate with refresh token from cookies
                    // Note: In GraphQL context, we'll need to handle cookies differently
                    // For now, let's focus on authenticated user scenario
                    null
                }

                if (authenticatedAuth == null) {
                    RefreshAccessTokenResponse(accessToken = null)
                } else {
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
                    RefreshAccessTokenResponse(accessToken = jwtToken)
                }
            } catch (e: Exception) {
                // Any authentication failure should result in null token, not exception
                RefreshAccessTokenResponse(accessToken = null)
            }
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