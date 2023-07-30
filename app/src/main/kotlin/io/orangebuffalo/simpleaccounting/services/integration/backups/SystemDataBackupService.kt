package io.orangebuffalo.simpleaccounting.services.integration.backups

import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
private val fileNameDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")

/**
 * Performs system data backup.
 */
@Service
class SystemDataBackupService(
    private val jdbcTemplate: JdbcTemplate,
    private val backupProvider: BackupProvider,
    private val backupProperties: BackupProperties,
) {

    @Scheduled(fixedDelayString = "\${simpleaccounting.backup.scheduling-delay-in-hours}", timeUnit = TimeUnit.HOURS)
    fun executeBackup() = runBlocking {
        if (!backupProperties.enabled) {
            logger.info { "Backup is disabled" }
            return@runBlocking
        }

        logger.info { "Starting system data backup" }

        val backupFile = getBackupFile()

        try {
            withDbContext {
                jdbcTemplate.execute("script drop to '${backupFile.toAbsolutePath()}' compression zip")
            }
            logger.debug { "Database saved to $backupFile" }

            backupProvider.acceptBackup(backupFile)
            logger.debug { "Backup file $backupFile accepted by backup provider" }
        } finally {
            Files.delete(backupFile)
        }

        logger.info { "System data backup completed" }
    }

    private fun getBackupFile(): Path {
        val currentTime = LocalDateTime.now().format(fileNameDateFormatter)
        val fileName = "simple-accounting-backup-$currentTime.zip"
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        return tempDir.resolve(fileName)
    }
}
