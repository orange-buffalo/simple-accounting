package io.orangebuffalo.simpleaccounting.infra.backups

import java.nio.file.Path

/**
 * Processes the backup files (typically, uploads to a remote storage).
 */
interface BackupProvider {

    /**
     * Accepts the backup file and processes it.
     */
    suspend fun acceptBackup(backupFile: Path)
}
