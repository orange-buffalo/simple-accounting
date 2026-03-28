package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContextFactory
import graphql.GraphQLContext
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@PublishedApi
internal val APPLICATION_CONTEXT_KEY = ApplicationContext::class

@Component
class SaGraphQLContextFactory(
    private val applicationContext: ApplicationContext,
) : SpringGraphQLContextFactory() {
    override suspend fun generateContext(request: ServerRequest): GraphQLContext {
        return GraphQLContext.newContext()
            .put("serverHttpRequest", request.exchange().request)
            .put(APPLICATION_CONTEXT_KEY, applicationContext)
            .build()
    }
}

inline fun <reified T : Any> GraphQLContext.getBean(): T =
    get<ApplicationContext>(APPLICATION_CONTEXT_KEY).getBean(T::class.java)
