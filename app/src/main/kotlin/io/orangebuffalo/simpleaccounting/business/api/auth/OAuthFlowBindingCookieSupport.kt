package io.orangebuffalo.simpleaccounting.business.api.auth

import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAUTH_FLOW_LIFETIME
import io.orangebuffalo.simpleaccounting.infra.graphql.GraphQlHttpRequestContext
import org.springframework.http.ResponseCookie
import java.time.Duration

const val OAUTH_FLOW_BINDING_COOKIE = "oauthFlowBinding"

/**
 * Hands the browser the secret that binds the pending OAuth2 flow to it. The authorization server
 * never sees this value, so only the browser that started the flow is able to complete it.
 */
internal fun DataFetchingEnvironment.addOAuthFlowBindingCookie(browserBinding: String) =
    addOAuthFlowBindingCookie(browserBinding, OAUTH_FLOW_LIFETIME)

/**
 * Drops the binding cookie once the flow is over, so that it does not linger in the browser.
 */
internal fun DataFetchingEnvironment.removeOAuthFlowBindingCookie() =
    addOAuthFlowBindingCookie("", Duration.ZERO)

private fun DataFetchingEnvironment.addOAuthFlowBindingCookie(value: String, maxAge: Duration) {
    graphQlContext.get<GraphQlHttpRequestContext>(GraphQlHttpRequestContext::class).addResponseCookie(
        ResponseCookie
            .from(OAUTH_FLOW_BINDING_COOKIE, value)
            .httpOnly(true)
            .sameSite("Strict")
            .path("/api")
            // todo #67: secure based on configuration
            .maxAge(maxAge)
            .build()
    )
}

internal fun DataFetchingEnvironment.oauthFlowBinding(): String? =
    graphQlContext.get<GraphQlHttpRequestContext>(GraphQlHttpRequestContext::class).oauthFlowBinding
