package io.orangebuffalo.simpleaccounting.infra.backups.impl

import io.orangebuffalo.simpleaccounting.infra.backups.BackupProperties
import io.orangebuffalo.simpleaccounting.infra.backups.BackupProvider
import io.orangebuffalo.simpleaccounting.infra.thirdparty.dropbox.DropboxApiClient
import io.orangebuffalo.simpleaccounting.infra.thirdparty.dropbox.FileListFolderEntry
import java.nio.file.Path
import kotlin.io.path.name

private const val BACKUP_FOLDER = "/backups"

class DropboxBackupProvider(
    private val backupProperties: BackupProperties
) : BackupProvider {
    override suspend fun acceptBackup(backupFile: Path) = withClient { client ->
        client.uploadFile(backupFile, "$BACKUP_FOLDER/${backupFile.name}")
        val existingBackups = client.listFolder(BACKUP_FOLDER)
            .filterIsInstance<FileListFolderEntry>()
            .sortedBy { it.clientModified }
        if (existingBackups.size > backupProperties.maxBackups) {
            val backupsToDelete = existingBackups
                .subList(0, existingBackups.size - backupProperties.maxBackups)
                .map { it.path }
            client.deleteFiles(backupsToDelete)
        }
    }

    private suspend fun withClient(block: suspend (client: DropboxApiClient) -> Unit) {
        val accessToken = backupProperties.dropbox.accessToken
        val refreshToken = backupProperties.dropbox.refreshToken
        val clientId = backupProperties.dropbox.clientId
        val clientSecret = backupProperties.dropbox.clientSecret
        require(accessToken != null && refreshToken != null && clientId != null && clientSecret != null) {
            "Dropbox tokens are not configured"
        }
        DropboxApiClient(
            accessToken = accessToken,
            refreshToken = refreshToken,
            clientId = clientId,
            clientSecret = clientSecret
        ).use { client ->
            block(client)
        }
    }
}
