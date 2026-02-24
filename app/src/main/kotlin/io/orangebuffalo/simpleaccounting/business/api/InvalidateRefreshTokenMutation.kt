package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.getServerWebExchange
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class InvalidateRefreshTokenMutation : Mutation {
    @Suppress("unused")
    @GraphQLDescription("Invalidates the refresh token cookie, effectively logging out the current user.")
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    suspend fun invalidateRefreshToken(): Boolean {
        val exchange = getServerWebExchange()
        exchange.response.addCookie(
            ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .sameSite("Strict")
                .path("/api/auth/token")
                .maxAge(Duration.ZERO)
                .build()
        )
        return true
    }
}
