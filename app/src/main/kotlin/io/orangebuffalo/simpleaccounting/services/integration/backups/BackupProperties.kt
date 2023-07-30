package io.orangebuffalo.simpleaccounting.services.integration.backups

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("simpleaccounting.backup")
@Component
data class BackupProperties(
    var enabled: Boolean = false,
    var maxBackups: Int = 50,
    var dropbox: DropboxBackupProperties = DropboxBackupProperties(),
) {

    data class DropboxBackupProperties(
        var accessToken: String? = null,
        var refreshToken: String? = null,
        var clientId: String? = null,
        var clientSecret: String? = null,
    )
}
