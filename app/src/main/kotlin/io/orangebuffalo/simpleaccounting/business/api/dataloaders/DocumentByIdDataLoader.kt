package io.orangebuffalo.simpleaccounting.business.api.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

private const val NAME = "documentById"

@Component
class DocumentByIdDataLoader(
    private val documentsRepository: DocumentsRepository,
) : KotlinDataLoader<Long, DocumentGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<Long, DocumentGqlDto?> =
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

fun DataFetchingEnvironment.loadDocumentById(
    documentId: Long,
): CompletableFuture<DocumentGqlDto?> = getDataLoader<Long, DocumentGqlDto?>(NAME)!!.load(documentId)
