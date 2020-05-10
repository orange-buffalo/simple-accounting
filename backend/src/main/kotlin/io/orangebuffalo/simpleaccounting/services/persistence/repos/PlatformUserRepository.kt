package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser

interface PlatformUserRepository
    : AbstractEntityRepository<PlatformUser>, PlatformUserRepositoryExt

interface PlatformUserRepositoryExt {
    fun findByUserName(userName: String): PlatformUser?
}
