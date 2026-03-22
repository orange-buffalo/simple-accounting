package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.documents.storage.local.LocalFileSystemDocumentsStorageProperties
import org.springframework.stereotype.Component

@Component
class SystemSettingsQuery(
    private val localFileSystemDocumentsStorageProperties: LocalFileSystemDocumentsStorageProperties,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns the system settings.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    suspend fun systemSettings(): SystemSettings {
        return SystemSettings(
            localFileSystemDocumentsStorageEnabled = localFileSystemDocumentsStorageProperties.enabled,
        )
    }

    @GraphQLDescription("System-wide settings.")
    data class SystemSettings(
        @GraphQLDescription("Whether local file system documents storage is enabled.")
        val localFileSystemDocumentsStorageEnabled: Boolean,
    )
}
