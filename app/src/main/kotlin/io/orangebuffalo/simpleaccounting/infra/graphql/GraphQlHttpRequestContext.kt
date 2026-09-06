package io.orangebuffalo.simpleaccounting.infra.graphql

import org.springframework.http.ResponseCookie

class GraphQlHttpRequestContext(
    val refreshToken: String?,
    /**
     * Binds a pending OAuth2 authentication flow to the browser that has started it.
     * @see io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationService
     */
    val oauthFlowBinding: String?,
) {
    private val mutableResponseCookies = mutableListOf<ResponseCookie>()

    val responseCookies: List<ResponseCookie>
        get() = mutableResponseCookies.toList()

    fun addResponseCookie(cookie: ResponseCookie) {
        mutableResponseCookies.add(cookie)
    }
}
