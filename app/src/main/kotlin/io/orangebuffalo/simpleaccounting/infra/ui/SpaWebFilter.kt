package io.orangebuffalo.simpleaccounting.infra.ui

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.web.filter.OncePerRequestFilter

open class SpaWebFilter : OncePerRequestFilter() {

    private val actuatorEndpointRequestMatcher = EndpointRequest.toAnyEndpoint()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        return actuatorEndpointRequestMatcher.matches(request) ||
            path == "/favicon.ico" ||
            path == "/api" || path.startsWith("/api/") ||
            path == "/assets" || path.startsWith("/assets/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.getRequestDispatcher("/index.html").forward(request, response)
    }
}
