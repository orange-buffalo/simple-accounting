package io.orangebuffalo.simpleaccounting.business.documents.migration

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface DocumentsMigrationRepository : AbstractEntityRepository<DocumentsMigration> {
    fun existsByUserIdAndCompletedAtIsNull(userId: String): Boolean
}
