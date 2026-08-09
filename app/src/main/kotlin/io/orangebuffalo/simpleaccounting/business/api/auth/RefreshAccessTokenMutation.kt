package io.orangebuffalo.simpleaccounting.business.api.auth

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshAuthenticationToken
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import io.orangebuffalo.simpleaccounting.infra.graphql.GraphQlHttpRequestContext
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import io.orangebuffalo.simpleaccounting.infra.graphql.SUBSCRIPTION_AUTHENTICATION_KEY
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class RefreshAccessTokenMutation(
    private val jwtService: JwtService,
    private val workspaceAccessTokensService: WorkspaceAccessTokensService,
    private val authenticationManager: AuthenticationManager
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription(
        "Refreshes the access token using the refresh token from cookies or current authentication. " +
                "Returns a response with either a valid access token or null if authentication fails."
    )
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    fun refreshAccessToken(
        env: DataFetchingEnvironment
    ): RefreshAccessTokenResponse {
        val currentAuth = env.graphQlContext.get<Authentication?>(SUBSCRIPTION_AUTHENTICATION_KEY)

        val refreshToken = env.graphQlContext.get<GraphQlHttpRequestContext>(GraphQlHttpRequestContext::class).refreshToken

        val principal = when {
            currentAuth?.isAuthenticated == true && currentAuth.principal is SecurityPrincipal ->
                currentAuth.principal as SecurityPrincipal

            refreshToken != null -> {
                try {
                    val authenticationToken = RefreshAuthenticationToken(refreshToken)
                    authenticationManager.authenticate(authenticationToken).principal as? SecurityPrincipal
                } catch (e: AuthenticationException) {
                    null
                }
            }

            else -> null
        }

        if (principal == null) {
            return RefreshAccessTokenResponse(accessToken = null)
        }

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

    @GraphQLDescription("Response for refreshing access token.")
    data class RefreshAccessTokenResponse(
        @GraphQLDescription(
            "The new access token if authentication was successful, null otherwise."
        )
        val accessToken: String?
    )
}
