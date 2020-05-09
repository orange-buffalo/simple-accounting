package io.orangebuffalo.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.simpleaccounting.services.persistence.repos.LegacyAbstractEntityRepository

interface GoogleDriveStorageIntegrationRepository : LegacyAbstractEntityRepository<GoogleDriveStorageIntegration> {

    fun findByUserId(userId: Long): GoogleDriveStorageIntegration?
}
