package io.orangebuffalo.simpleaccounting.infra.backups.impl

import io.orangebuffalo.simpleaccounting.infra.backups.BackupProvider
import mu.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class NoOpBackupProvider : BackupProvider {
    override suspend fun acceptBackup(backupFile: Path) {
        logger.info { "Backup file $backupFile is ignored by noop provider" }
    }
}
