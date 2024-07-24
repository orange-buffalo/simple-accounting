package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface PlatformUserRepository : AbstractEntityRepository<PlatformUser> {
    fun findByUserName(userName: String): PlatformUser?
}
