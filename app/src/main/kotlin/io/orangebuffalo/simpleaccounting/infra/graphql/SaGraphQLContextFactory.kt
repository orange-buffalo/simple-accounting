package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContextFactory
import graphql.GraphQLContext
import io.orangebuffalo.simpleaccounting.business.security.ProgrammaticAuthentication
import io.orangebuffalo.simpleaccounting.business.security.SpringSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import kotlinx.coroutines.reactor.ReactorContext
import mu.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

private val logger = KotlinLogging.logger {}

@PublishedApi
internal val APPLICATION_CONTEXT_KEY = ApplicationContext::class

@Component
class SaGraphQLContextFactory(
    private val applicationContext: ApplicationContext,
    private val jwtService: JwtService,
) : SpringGraphQLContextFactory() {
    override suspend fun generateContext(request: ServerRequest): GraphQLContext {
        val contextBuilder = GraphQLContext.newContext()
            .put("serverHttpRequest", request.exchange().request)
            .put(APPLICATION_CONTEXT_KEY, applicationContext)

        val authentication = extractAuthentication(request)
        if (authentication != null) {
            val currentReactorContext = coroutineContext[ReactorContext]?.context
                ?: reactor.util.context.Context.empty()
            val authReactorContext = currentReactorContext.putAll(
                ReactiveSecurityContextHolder.withAuthentication(authentication)
            )
            contextBuilder.put(CoroutineContext::class, ReactorContext(authReactorContext))
        }

        return contextBuilder.build()
    }

    private fun extractAuthentication(request: ServerRequest): ProgrammaticAuthentication? {
        val authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION) ?: return null
        if (!authHeader.startsWith("Bearer ")) return null
        val token = authHeader.removePrefix("Bearer ").trim()
        return try {
            val userDetails = jwtService.validateTokenAndBuildUserDetails(token)
            if (userDetails is SpringSecurityPrincipal) ProgrammaticAuthentication(userDetails) else null
        } catch (e: BadCredentialsException) {
            logger.debug(e) { "Failed to validate JWT token from HTTP request" }
            null
        }
    }
}

inline fun <reified T : Any> GraphQLContext.getBean(): T =
    get<ApplicationContext>(APPLICATION_CONTEXT_KEY).getBean(T::class.java)
