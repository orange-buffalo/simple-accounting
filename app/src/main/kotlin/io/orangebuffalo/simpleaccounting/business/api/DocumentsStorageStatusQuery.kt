package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.directives.SlowOperation
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import org.springframework.stereotype.Component

@Component
class DocumentsStorageStatusQuery(
    private val documentsService: DocumentsService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns the current user's documents storage status.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    @SlowOperation
    suspend fun documentsStorageStatus(): DocumentsStorageStatusResponse {
        val status = documentsService.getCurrentUserStorageStatus()
        return DocumentsStorageStatusResponse(active = status.active)
    }

    @GraphQLDescription("Documents storage status for the current user.")
    data class DocumentsStorageStatusResponse(
        @param:GraphQLDescription("Whether the documents storage is active and available.")
        val active: Boolean,
    )
}
