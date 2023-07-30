package io.orangebuffalo.simpleaccounting.services.integration.backups.impl

import io.orangebuffalo.simpleaccounting.services.integration.backups.BackupProvider
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class NoOpBackupProvider : BackupProvider {
    override suspend fun acceptBackup(backupFile: java.nio.file.Path) {
        logger.info { "Backup file $backupFile is ignored by noop provider" }
    }
}
