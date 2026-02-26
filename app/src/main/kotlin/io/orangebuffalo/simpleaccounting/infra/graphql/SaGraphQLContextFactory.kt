package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContextFactory
import graphql.GraphQLContext
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class SaGraphQLContextFactory : SpringGraphQLContextFactory() {
    override suspend fun generateContext(request: ServerRequest): GraphQLContext {
        return GraphQLContext.newContext()
            .put("serverHttpRequest", request.exchange().request)
            .build()
    }
}
