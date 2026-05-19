package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class DocumentsMigrationsQuery(
    private val paginationService: GraphqlPaginationService,
    private val dslContext: DSLContext,
) : Query {

    @Suppress("unused")
    @GraphQLDescription(
        "Returns documents migration tasks for the current user with cursor-based pagination. " +
            "Results are sorted by creation time descending by default, newest first."
    )
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun documentsMigrations(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
    ): ConnectionGqlDto<DocumentsMigrationGqlDto> {
        val documentsMigration = Tables.DOCUMENTS_MIGRATION
        val documentsMigrationDocument = Tables.DOCUMENTS_MIGRATION_DOCUMENT
        return paginationService.forTable(documentsMigration)
            .applyCurrentUserFiltering { user -> documentsMigration.userId.eq(user.id) }
            .page(
                first = first,
                after = after,
                mapQueryRecord = { record ->
                    DocumentsMigrationGqlDto(
                        id = record[documentsMigration.id]!!,
                        requestedDocumentsCount = 0,
                        migratedDocumentsCount = record[documentsMigration.migratedDocumentsCount]!!,
                        completedAt = record[documentsMigration.completedAt],
                        documentIdsToMigrate = emptyList(),
                    )
                },
                postProcess = { migrations ->
                    val documentsByMigrationId = dslContext
                        .select(documentsMigrationDocument.migrationId, documentsMigrationDocument.documentId)
                        .from(documentsMigrationDocument)
                        .where(documentsMigrationDocument.migrationId.`in`(migrations.map { it.id }))
                        .fetch()
                        .groupBy(
                            { it[documentsMigrationDocument.migrationId]!! },
                            { it[documentsMigrationDocument.documentId]!! },
                        )

                    migrations.map { migration ->
                        val documentIds = documentsByMigrationId[migration.id]?.sorted() ?: emptyList()
                        migration.copy(
                            requestedDocumentsCount = documentIds.size,
                            documentIdsToMigrate = documentIds,
                        )
                    }
                },
            )
    }
}
