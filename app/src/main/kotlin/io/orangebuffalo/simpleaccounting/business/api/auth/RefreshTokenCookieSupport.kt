package io.orangebuffalo.simpleaccounting.business.api.auth

import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.security.remeberme.TOKEN_LIFETIME_IN_DAYS
import io.orangebuffalo.simpleaccounting.infra.graphql.GraphQlHttpRequestContext
import org.springframework.http.ResponseCookie
import java.time.Duration

/**
 * Issues the cookie that allows the browser to obtain new access tokens without re-authenticating.
 */
internal fun DataFetchingEnvironment.addRefreshTokenCookie(refreshToken: String) {
    graphQlContext.get<GraphQlHttpRequestContext>(GraphQlHttpRequestContext::class).addResponseCookie(
        ResponseCookie
            .from("refreshToken", refreshToken)
            .httpOnly(true)
            .sameSite("Strict")
            .path("/api")
            // todo #67: secure based on configuration
            .maxAge(Duration.ofDays(TOKEN_LIFETIME_IN_DAYS))
            .build()
    )
}
