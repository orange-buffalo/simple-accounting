package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import java.util.*

interface PlatformUserRepository : CrudRepository<PlatformUser, Long> {

    fun findByUserName(userName: String): Optional<PlatformUser>

    fun findAll(pageable: Pageable): Page<PlatformUser>
}