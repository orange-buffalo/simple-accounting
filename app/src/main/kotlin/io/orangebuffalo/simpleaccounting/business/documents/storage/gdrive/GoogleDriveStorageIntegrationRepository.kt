package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface GoogleDriveStorageIntegrationRepository : AbstractEntityRepository<GoogleDriveStorageIntegration> {
    fun findByUserId(userId: Long): GoogleDriveStorageIntegration?
}
