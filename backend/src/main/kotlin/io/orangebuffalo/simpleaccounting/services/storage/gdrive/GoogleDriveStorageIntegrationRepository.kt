package io.orangebuffalo.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.repos.LegacyAbstractEntityRepository

interface GoogleDriveStorageIntegrationRepository : LegacyAbstractEntityRepository<GoogleDriveStorageIntegration> {

    fun findByUser(user: PlatformUser): GoogleDriveStorageIntegration?
}
