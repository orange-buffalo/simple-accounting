package io.orangebuffalo.simpleaccounting.business.api.documents

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import com.expediagroup.graphql.dataloader.instrumentation.extensions.dispatchIfNeeded
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import kotlinx.coroutines.future.await
import org.dataloader.DataLoader
import org.springframework.stereotype.Component

private const val NAME = "documentsByIds"

@Component
class DocumentsByIdsDataLoader(
    private val documentsRepository: DocumentsRepository,
) : KotlinDataLoader<List<Long>, List<DocumentGqlDto>> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<List<Long>, List<DocumentGqlDto>> =
        newAsyncMappedDataLoader { idLists ->
            val allDocumentIds = idLists.flatten().toSet()
            val documents = documentsRepository.findAllById(allDocumentIds)
            val usagesByDocId = documentsRepository.findUsagesByDocumentIds(allDocumentIds)
            val documentDtoById = documents.associate { document ->
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
            idLists.associateWith { ids -> ids.mapNotNull { id -> documentDtoById[id] } }
        }
}

suspend fun DataFetchingEnvironment.loadDocumentsByIds(
    documentIds: List<Long>,
): List<DocumentGqlDto> = getDataLoader<List<Long>, List<DocumentGqlDto>>(NAME)!!
    .load(documentIds)
    .dispatchIfNeeded(this)
    .await()
