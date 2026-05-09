package io.orangebuffalo.simpleaccounting.infra.backups

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * System data backup configuration.
 */
@ConfigurationProperties("sa.backup")
@Component
data class BackupProperties(
    /**
     * Whether scheduled system data backups are enabled.
     */
    var enabled: Boolean = false,

    /**
     * Delay between scheduled backup attempts, in hours.
     */
    var schedulingDelayInHours: Long = 12,

    /**
     * Maximum number of backup files to retain in the remote backup storage.
     */
    var maxBackups: Int = 50,

    /**
     * Dropbox backup provider configuration.
     */
    var dropbox: DropboxBackupProperties = DropboxBackupProperties(),
) {

    /**
     * Dropbox backup provider configuration.
     */
    data class DropboxBackupProperties(
        /**
         * Whether Dropbox should be used as the backup provider.
         */
        var active: Boolean = false,

        /**
         * Dropbox OAuth access token.
         */
        var accessToken: String? = null,

        /**
         * Dropbox OAuth refresh token.
         */
        var refreshToken: String? = null,

        /**
         * Dropbox OAuth client id.
         */
        var clientId: String? = null,

        /**
         * Dropbox OAuth client secret.
         */
        var clientSecret: String? = null,
    )
}
