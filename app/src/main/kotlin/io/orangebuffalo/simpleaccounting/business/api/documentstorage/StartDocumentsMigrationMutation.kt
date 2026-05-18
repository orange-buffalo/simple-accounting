package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.documents.loadDocumentsByIdsAsync
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigration
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigrationService
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsStorageNotConfiguredException
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Component
class StartDocumentsMigrationMutation(
    private val documentsMigrationService: DocumentsMigrationService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Starts migration of documents that are not stored in the current upload storage.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    @BusinessError(
        exceptionClass = DocumentsStorageNotConfiguredException::class,
        errorCode = "DOCUMENTS_STORAGE_NOT_CONFIGURED",
        errorCodeDescription = "Documents storage is not configured for the current user.",
    )
    suspend fun startDocumentsMigration(): DocumentsMigrationGqlDto {
        return documentsMigrationService.startDocumentsMigration().toGqlDto()
    }
}

@GraphQLName("DocumentsMigration")
@GraphQLDescription("Documents migration task for the current user.")
data class DocumentsMigrationGqlDto(
    @GraphQLDescription("ID of the documents migration task.")
    val id: String,
    @GraphQLDescription("Number of documents already migrated.")
    val migratedDocumentsCount: Int,
    @GraphQLDescription("Time when the migration was completed.")
    val completedAt: Instant?,
    @GraphQLIgnore val documentIdsToMigrate: List<String>,
) {
    @GraphQLDescription("Documents that should be migrated.")
    fun documentsToMigrate(env: DataFetchingEnvironment): CompletableFuture<List<DocumentGqlDto>> {
        return env.loadDocumentsByIdsAsync(documentIdsToMigrate)
    }
}

private fun DocumentsMigration.toGqlDto() = DocumentsMigrationGqlDto(
    id = id!!,
    migratedDocumentsCount = migratedDocumentsCount,
    completedAt = completedAt,
    documentIdsToMigrate = documentsToMigrate.map { it.documentId }.sorted(),
)
