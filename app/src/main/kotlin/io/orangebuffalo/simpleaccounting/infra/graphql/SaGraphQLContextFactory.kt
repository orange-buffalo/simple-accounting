package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContextFactory
import graphql.GraphQLContext
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class SaGraphQLContextFactory(
    private val documentsService: DocumentsService,
) : SpringGraphQLContextFactory() {
    override suspend fun generateContext(request: ServerRequest): GraphQLContext {
        return GraphQLContext.newContext()
            .put("serverHttpRequest", request.exchange().request)
            .put(DocumentsService::class, documentsService)
            .build()
    }
}
