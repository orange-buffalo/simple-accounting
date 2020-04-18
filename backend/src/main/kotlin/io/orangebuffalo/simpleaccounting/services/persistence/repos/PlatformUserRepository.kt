package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser

interface PlatformUserRepository : LegacyAbstractEntityRepository<PlatformUser> {

    fun findByUserName(userName: String): PlatformUser?
}
