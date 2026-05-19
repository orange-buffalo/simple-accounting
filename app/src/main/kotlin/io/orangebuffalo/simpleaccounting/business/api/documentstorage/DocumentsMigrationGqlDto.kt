package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.documents.loadDocumentsByIdsAsync
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigration
import java.time.Instant
import java.util.concurrent.CompletableFuture

@GraphQLName("DocumentsMigration")
@GraphQLDescription("Documents migration task for the current user.")
data class DocumentsMigrationGqlDto(
    @GraphQLDescription("ID of the documents migration task.")
    val id: String,
    @GraphQLDescription("Number of documents requested for migration.")
    val requestedDocumentsCount: Int,
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

fun DocumentsMigration.toGqlDto() = DocumentsMigrationGqlDto(
    id = id!!,
    requestedDocumentsCount = documentsToMigrate.size,
    migratedDocumentsCount = migratedDocumentsCount,
    completedAt = completedAt,
    documentIdsToMigrate = documentsToMigrate.map { it.documentId }.sorted(),
)
