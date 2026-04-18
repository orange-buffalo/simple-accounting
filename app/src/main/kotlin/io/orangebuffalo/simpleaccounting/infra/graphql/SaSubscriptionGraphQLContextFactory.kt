package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.spring.subscriptions.SpringGraphQLSubscriptionHooks
import com.expediagroup.graphql.server.spring.subscriptions.SpringSubscriptionGraphQLContextFactory
import graphql.GraphQLContext
import io.orangebuffalo.simpleaccounting.business.security.ProgrammaticAuthentication
import io.orangebuffalo.simpleaccounting.business.security.SpringSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import mu.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession

private val logger = KotlinLogging.logger {}

internal val SUBSCRIPTION_AUTHENTICATION_KEY = "sa-subscription-authentication"

@Component
class SaSubscriptionGraphQLContextFactory(
    private val applicationContext: ApplicationContext,
) : SpringSubscriptionGraphQLContextFactory {

    override suspend fun generateContext(
        session: WebSocketSession,
        params: Any?,
    ): GraphQLContext {
        return GraphQLContext.newContext()
            .put(APPLICATION_CONTEXT_KEY, applicationContext)
            .build()
    }
}

@Component
class SaSubscriptionHooks(
    private val jwtService: JwtService,
) : SpringGraphQLSubscriptionHooks {

    override fun onConnect(
        connectionParams: Any?,
        session: WebSocketSession,
        graphQLContext: GraphQLContext,
    ): GraphQLContext {
        val token = extractToken(connectionParams)
        if (token != null) {
            try {
                val userDetails = jwtService.validateTokenAndBuildUserDetails(token)
                if (userDetails is SpringSecurityPrincipal) {
                    graphQLContext.put(
                        SUBSCRIPTION_AUTHENTICATION_KEY,
                        ProgrammaticAuthentication(userDetails) as Authentication
                    )
                }
            } catch (e: BadCredentialsException) {
                logger.debug(e) { "Failed to validate JWT token from subscription connectionParams" }
            }
        }
        return graphQLContext
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractToken(connectionParams: Any?): String? {
        val params = connectionParams as? Map<String, Any?> ?: return null
        return params["token"] as? String
    }
}
