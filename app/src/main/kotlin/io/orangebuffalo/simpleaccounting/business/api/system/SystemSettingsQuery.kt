package io.orangebuffalo.simpleaccounting.business.api.system

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.documents.storage.local.LocalFileSystemDocumentsStorageProperties
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAUTH_IDENTITY_CALLBACK_PATH
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingProperties
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import org.springframework.stereotype.Component

@Component
class SystemSettingsQuery(
    private val localFileSystemDocumentsStorageProperties: LocalFileSystemDocumentsStorageProperties,
    private val simpleAccountingProperties: SimpleAccountingProperties,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns the system settings.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    fun systemSettings(): SystemSettings {
        return SystemSettings(
            localFileSystemDocumentsStorageEnabled = localFileSystemDocumentsStorageProperties.enabled,
            oauthCallbackUrl = "${simpleAccountingProperties.publicUrl}$OAUTH_IDENTITY_CALLBACK_PATH",
        )
    }

    @GraphQLDescription("System-wide settings.")
    data class SystemSettings(
        @GraphQLDescription("Whether local file system documents storage is enabled.")
        val localFileSystemDocumentsStorageEnabled: Boolean,
        @GraphQLDescription(
            "Redirect URL that must be registered at OAuth2 providers for authentication flows."
        )
        val oauthCallbackUrl: String,
    )
}
