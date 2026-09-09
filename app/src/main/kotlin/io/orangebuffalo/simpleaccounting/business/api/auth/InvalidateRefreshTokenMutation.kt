package io.orangebuffalo.simpleaccounting.business.api.auth

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.infra.graphql.GraphQlHttpRequestContext
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class InvalidateRefreshTokenMutation(
    private val refreshTokensService: RefreshTokensService,
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription("Revokes the refresh token and clears its cookie, effectively logging out the current user.")
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    fun invalidateRefreshToken(env: DataFetchingEnvironment): Boolean {
        val requestContext = env.graphQlContext.get<GraphQlHttpRequestContext>(GraphQlHttpRequestContext::class)
        requestContext.refreshToken?.let(refreshTokensService::revokeToken)
        requestContext.addResponseCookie(
            ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .sameSite("Strict")
                .path("/api")
                .maxAge(Duration.ZERO)
                .build()
        )
        return true
    }
}
