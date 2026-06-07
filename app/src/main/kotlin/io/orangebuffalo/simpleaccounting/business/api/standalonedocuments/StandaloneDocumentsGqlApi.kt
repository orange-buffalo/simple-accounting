package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.springframework.stereotype.Component

@Component
class StandaloneDocumentsGqlApi(
    private val paginationService: GraphqlPaginationService,
) {
    private val standaloneDocument = Tables.STANDALONE_DOCUMENT
    private val document = Tables.DOCUMENT

    suspend fun loadStandaloneDocuments(
        workspaceId: String,
        first: Int,
        after: String?,
    ): ConnectionGqlDto<StandaloneDocumentGqlDto> = paginationService
        .forTable(standaloneDocument)
        .onQuery { it.join(document).on(document.id.eq(standaloneDocument.documentId)) }
        .addPredicate(document.workspaceId.eq(workspaceId))
        .page(first, after) { record ->
            StandaloneDocumentGqlDto(
                id = record[standaloneDocument.id]!!,
                version = record[standaloneDocument.version]!!,
                title = record[standaloneDocument.title]!!,
                documentId = record[standaloneDocument.documentId]!!,
            )
        }
}
