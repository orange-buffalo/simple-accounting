package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface PlatformUsersRepository : AbstractEntityRepository<PlatformUser> {
    fun findByUserName(userName: String): PlatformUser?
}
