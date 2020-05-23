package io.orangebuffalo.simpleaccounting.services.storage.gdrive.impl

import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.services.storage.gdrive.GoogleDriveStorageIntegrationRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class GoogleDriveStorageIntegrationRepositoryExtImpl(
    private val dslContext: DSLContext
) : GoogleDriveStorageIntegrationRepositoryExt {

    private val integration = Tables.GOOGLE_DRIVE_STORAGE_INTEGRATION

    override fun findByUserId(userId: Long): GoogleDriveStorageIntegration? = dslContext
        .select()
        .from(integration)
        .where(integration.userId.eq(userId))
        .fetchOneOrNull()
}
