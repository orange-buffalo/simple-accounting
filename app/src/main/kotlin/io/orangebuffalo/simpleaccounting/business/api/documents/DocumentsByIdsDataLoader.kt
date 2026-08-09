package io.orangebuffalo.simpleaccounting.business.api.documents

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import io.orangebuffalo.simpleaccounting.infra.graphql.loadManyAndDispatch
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

private const val NAME = "documentsByIds"

@Component
class DocumentsByIdsDataLoader(
    private val documentsRepository: DocumentsRepository,
) : KotlinDataLoader<String, DocumentGqlDto> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<String, DocumentGqlDto> =
        newAsyncMappedDataLoader { documentIds ->
            val documents = documentsRepository.findAllById(documentIds)
            val usagesByDocId = documentsRepository.findUsagesByDocumentIds(documentIds)
            documents.associate { document ->
                document.id!! to DocumentGqlDto(
                    id = document.id!!,
                    version = document.version!!,
                    name = document.name,
                    timeUploaded = document.timeUploaded,
                    sizeInBytes = document.sizeInBytes,
                    storageId = document.storageId,
                    mimeType = document.mimeType,
                    usedBy = usagesByDocId[document.id] ?: emptyList(),
                )
            }
        }
}

fun DataFetchingEnvironment.loadDocumentsByIds(
    documentIds: List<String>
): CompletableFuture<List<DocumentGqlDto>> {
    return loadManyAndDispatch<String, DocumentGqlDto>(NAME, documentIds).thenApply { documents ->
        documents.filterNotNull()
            .sortedWith(compareBy(DocumentGqlDto::name, DocumentGqlDto::id))
    }
}
