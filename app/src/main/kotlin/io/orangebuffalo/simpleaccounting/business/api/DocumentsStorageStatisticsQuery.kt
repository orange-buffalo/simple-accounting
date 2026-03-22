package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import org.springframework.stereotype.Component

@Component
class DocumentsStorageStatisticsQuery(
    private val documentsService: DocumentsService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription(
        "Returns statistics about document storage usage across all workspaces of the current user. " +
                "Only storages that have at least one document are included."
    )
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    suspend fun documentsStorageStatistics(): List<DocumentsStorageStatisticsItem> {
        return documentsService.getDocumentsStorageStatistics()
            .map { DocumentsStorageStatisticsItem(storageId = it.storageId, documentsCount = it.documentsCount) }
    }

    @GraphQLDescription("Statistics about document storage usage.")
    data class DocumentsStorageStatisticsItem(
        @GraphQLDescription("The identifier of the document storage.")
        val storageId: String,
        @GraphQLDescription("The total number of documents stored in this storage across all workspaces of the current user.")
        val documentsCount: Int,
    )
}
