package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.dataloader.DataLoader
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceStandaloneDocumentKey(val workspaceId: String, val standaloneDocumentId: String)

private const val NAME = "standaloneDocumentByWorkspaceAndId"

@Component
class StandaloneDocumentByWorkspaceAndIdDataLoader(
    private val dslContext: DSLContext,
) : KotlinDataLoader<WorkspaceStandaloneDocumentKey, StandaloneDocumentGqlDto> {

    private val standaloneDocument = Tables.STANDALONE_DOCUMENT
    private val document = Tables.DOCUMENT

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceStandaloneDocumentKey, StandaloneDocumentGqlDto> =
        newAsyncMappedDataLoader { keys ->
            val requestedPairsPredicate = DSL.or(keys.map { key ->
                standaloneDocument.id.eq(key.standaloneDocumentId)
                    .and(document.workspaceId.eq(key.workspaceId))
            })

            val dtosByKey = dslContext
                .select(standaloneDocument.fields().toList() + document.workspaceId)
                .from(standaloneDocument)
                .join(document).on(document.id.eq(standaloneDocument.documentId))
                .where(requestedPairsPredicate)
                .fetch()
                .associate { record ->
                    val standaloneDocumentId = record[standaloneDocument.id]!!
                    val workspaceId = record[document.workspaceId]!!
                    WorkspaceStandaloneDocumentKey(workspaceId, standaloneDocumentId) to StandaloneDocumentGqlDto(
                        id = standaloneDocumentId,
                        version = record[standaloneDocument.version]!!,
                        title = record[standaloneDocument.title]!!,
                        documentId = record[standaloneDocument.documentId]!!,
                    )
                }

            dtosByKey
        }
}

fun DataFetchingEnvironment.loadStandaloneDocumentByWorkspaceAndId(
    workspaceId: String,
    standaloneDocumentId: String,
): CompletableFuture<StandaloneDocumentGqlDto?> =
    getDataLoader<WorkspaceStandaloneDocumentKey, StandaloneDocumentGqlDto>(NAME)!!
        .load(WorkspaceStandaloneDocumentKey(workspaceId, standaloneDocumentId)).thenApply { it }
