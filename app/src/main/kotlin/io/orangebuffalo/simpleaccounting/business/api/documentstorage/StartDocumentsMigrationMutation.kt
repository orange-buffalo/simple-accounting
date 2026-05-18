package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigration
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigrationService
import org.springframework.stereotype.Component

@Component
class StartDocumentsMigrationMutation(
    private val documentsMigrationService: DocumentsMigrationService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Starts migration of documents that are not stored in the current upload storage.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun startDocumentsMigration(): DocumentsMigrationGqlDto {
        return documentsMigrationService.startDocumentsMigration().toGqlDto()
    }
}

@GraphQLName("DocumentsMigration")
@GraphQLDescription("Documents migration task for the current user.")
data class DocumentsMigrationGqlDto(
    @GraphQLDescription("ID of the documents migration task.")
    val id: String,
    @GraphQLDescription("ID of the user who owns this migration task.")
    val userId: String,
    @GraphQLDescription("IDs of documents that should be migrated.")
    val documentsToMigrate: List<String>,
    @GraphQLDescription("Number of documents already migrated.")
    val migratedDocumentsCount: Int,
)

private fun DocumentsMigration.toGqlDto() = DocumentsMigrationGqlDto(
    id = id!!,
    userId = userId,
    documentsToMigrate = documentsToMigrate.map { it.documentId }.sorted(),
    migratedDocumentsCount = migratedDocumentsCount,
)
