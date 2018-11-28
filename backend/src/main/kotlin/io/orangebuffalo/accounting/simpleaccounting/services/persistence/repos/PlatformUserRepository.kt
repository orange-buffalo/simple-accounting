package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser

interface PlatformUserRepository : AbstractEntityRepository<PlatformUser> {
    
    fun findByUserName(userName: String): PlatformUser
}