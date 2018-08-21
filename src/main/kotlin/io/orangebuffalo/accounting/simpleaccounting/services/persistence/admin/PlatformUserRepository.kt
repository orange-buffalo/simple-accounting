package io.orangebuffalo.accounting.simpleaccounting.services.persistence.admin

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.admin.entities.PlatformUser
import org.springframework.data.repository.CrudRepository

interface PlatformUserRepository : CrudRepository<PlatformUser, Long?> {
}