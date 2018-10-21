package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import java.util.*

interface PlatformUserRepository : AbstractEntityRepository<PlatformUser> {

    fun findByUserName(userName: String): Optional<PlatformUser>
}