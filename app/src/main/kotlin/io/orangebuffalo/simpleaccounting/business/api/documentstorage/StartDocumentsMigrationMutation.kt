package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigrationAlreadyInProgressException
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigrationService
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigrationSourceStorageNotActiveException
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsStorageNotConfiguredException
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsUploadStorageNotActiveException
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
    @BusinessError(
        exceptionClass = DocumentsUploadStorageNotActiveException::class,
        errorCode = "DOCUMENTS_UPLOAD_STORAGE_NOT_ACTIVE",
        errorCodeDescription = "Current upload storage is not active for the current user.",
    )
    @BusinessError(
        exceptionClass = DocumentsMigrationSourceStorageNotActiveException::class,
        errorCode = "DOCUMENTS_MIGRATION_SOURCE_STORAGE_NOT_ACTIVE",
        errorCodeDescription = "At least one source storage required for migration is not active for the current user.",
    )
    @BusinessError(
        exceptionClass = DocumentsMigrationAlreadyInProgressException::class,
        errorCode = "DOCUMENTS_MIGRATION_ALREADY_IN_PROGRESS",
        errorCodeDescription = "The current user already has an incomplete documents migration.",
    )
    suspend fun startDocumentsMigration(): DocumentsMigrationGqlDto {
        return documentsMigrationService.startDocumentsMigration().toGqlDto()
    }
}
