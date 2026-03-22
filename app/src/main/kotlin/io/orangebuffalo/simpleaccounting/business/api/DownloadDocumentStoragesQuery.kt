package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.directives.SlowOperation
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import org.springframework.stereotype.Component

@Component
class DownloadDocumentStoragesQuery(
    private val documentsService: DocumentsService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription(
        "Returns document storages that are currently available for downloading documents. " +
                "Iterates over all storage implementations and checks their download availability " +
                "for the current user context."
    )
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_ACTOR)
    @SlowOperation
    suspend fun getDownloadDocumentStorages(): List<DownloadDocumentStorageResponse> {
        return documentsService.getDownloadAvailableStorages()
            .map { DownloadDocumentStorageResponse(id = it) }
    }

    @GraphQLDescription("A document storage available for downloading documents.")
    data class DownloadDocumentStorageResponse(
        @GraphQLDescription("The identifier of the document storage.")
        val id: String,
    )
}
