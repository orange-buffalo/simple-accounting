package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface PlatformUsersRepository : AbstractEntityRepository<PlatformUser> {
    fun findByUserName(userName: String): PlatformUser?
}
