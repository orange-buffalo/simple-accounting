package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.directives.SlowOperation
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveDocumentsStorage
import org.springframework.stereotype.Component

@Component
class GoogleDriveStorageIntegrationStatusQuery(
    private val googleDriveDocumentsStorage: GoogleDriveDocumentsStorage,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns the current user's Google Drive storage integration status.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    @SlowOperation
    suspend fun googleDriveStorageIntegrationStatus(): GoogleDriveStorageIntegrationStatusResponse {
        val status = googleDriveDocumentsStorage.getCurrentUserIntegrationStatus()
        return GoogleDriveStorageIntegrationStatusResponse(
            folderId = status.folderId,
            folderName = status.folderName,
            authorizationUrl = status.authorizationUrl,
            authorizationRequired = status.authorizationRequired,
        )
    }

    @GraphQLDescription("Google Drive storage integration status for the current user.")
    data class GoogleDriveStorageIntegrationStatusResponse(
        @GraphQLDescription("Whether Google Drive authorization is required to use the storage.")
        val authorizationRequired: Boolean,
        @GraphQLDescription(
            "The URL to authorize access to Google Drive. " +
                    "Present only when authorization is required."
        )
        val authorizationUrl: String?,
        @GraphQLDescription("The ID of the Google Drive folder used for storing documents.")
        val folderId: String?,
        @GraphQLDescription("The name of the Google Drive folder used for storing documents.")
        val folderName: String?,
    )
}
