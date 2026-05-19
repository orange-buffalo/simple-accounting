package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigrationService
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsStorageNotConfiguredException
import org.springframework.stereotype.Component

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
