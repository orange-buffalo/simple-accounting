package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.data.repository.CrudRepository

interface PlatformUserRepository : CrudRepository<PlatformUser, Long?>