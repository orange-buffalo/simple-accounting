package io.orangebuffalo.accounting.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface GoogleDriveStorageIntegrationRepository : AbstractEntityRepository<GoogleDriveStorageIntegration> {

    fun findByUser(user: PlatformUser): GoogleDriveStorageIntegration?

    fun findByAuthStateToken(token: String): GoogleDriveStorageIntegration?
}