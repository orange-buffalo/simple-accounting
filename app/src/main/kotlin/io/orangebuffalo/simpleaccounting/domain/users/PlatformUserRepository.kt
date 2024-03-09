package io.orangebuffalo.simpleaccounting.domain.users

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface PlatformUserRepository : AbstractEntityRepository<PlatformUser> {
    fun findByUserName(userName: String): PlatformUser?
}
