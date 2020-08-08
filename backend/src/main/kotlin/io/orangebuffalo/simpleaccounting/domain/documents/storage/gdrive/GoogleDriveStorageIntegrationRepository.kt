package io.orangebuffalo.simpleaccounting.domain.documents.storage.gdrive

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface GoogleDriveStorageIntegrationRepository : AbstractEntityRepository<GoogleDriveStorageIntegration> {
    fun findByUserId(userId: Long): GoogleDriveStorageIntegration?
}
