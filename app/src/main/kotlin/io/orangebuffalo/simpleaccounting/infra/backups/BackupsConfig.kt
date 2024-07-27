package io.orangebuffalo.simpleaccounting.infra.backups

import io.orangebuffalo.simpleaccounting.infra.backups.impl.DropboxBackupProvider
import io.orangebuffalo.simpleaccounting.infra.backups.impl.NoOpBackupProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BackupsConfig {

    @ConditionalOnProperty("simpleaccounting.backup.dropbox.active", havingValue = "true")
    @Configuration
    class DropboxBackupConfig {
        @Bean
        fun dropboxBackupProvider(backupProperties: BackupProperties) = DropboxBackupProvider(backupProperties)
    }

    @ConditionalOnMissingBean(BackupProvider::class)
    @Configuration
    class NoOpBackupConfig {
        @Bean
        fun noOpBackupProvider() = NoOpBackupProvider()
    }

}
