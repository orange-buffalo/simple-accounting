package io.orangebuffalo.simpleaccounting.business.api.auth

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.graphql.GraphQlHttpRequestContext
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class InvalidateRefreshTokenMutation : Mutation {
    @Suppress("unused")
    @GraphQLDescription("Invalidates the refresh token cookie, effectively logging out the current user.")
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    suspend fun invalidateRefreshToken(env: DataFetchingEnvironment): Boolean {
        env.graphQlContext.get<GraphQlHttpRequestContext>(GraphQlHttpRequestContext::class).addResponseCookie(
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
