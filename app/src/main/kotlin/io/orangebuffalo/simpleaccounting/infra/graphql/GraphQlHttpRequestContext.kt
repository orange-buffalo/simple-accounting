package io.orangebuffalo.simpleaccounting.infra.graphql

import org.springframework.http.ResponseCookie

class GraphQlHttpRequestContext(
    val refreshToken: String?,
) {
    private val mutableResponseCookies = mutableListOf<ResponseCookie>()

    val responseCookies: List<ResponseCookie>
        get() = mutableResponseCookies.toList()

    fun addResponseCookie(cookie: ResponseCookie) {
        mutableResponseCookies.add(cookie)
    }
}
