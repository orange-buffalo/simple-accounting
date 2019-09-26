package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.integration.ensureRegularUserPrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.PlatformUserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PlatformUserService(
    private val userRepository: PlatformUserRepository
) {

    suspend fun getCurrentUser(): PlatformUser =
        withDbContext {
            userRepository.findByUserName(ensureRegularUserPrincipal().userName)
                ?: throw IllegalStateException("Current principal is not resolved to a user")
        }

    suspend fun getUserByUserName(userName: String): PlatformUser? =
        withDbContext {
            userRepository.findByUserName(userName)
        }

    suspend fun getUsers(page: Pageable): Page<PlatformUser> =
        withDbContext {
            userRepository.findAll(page)
        }

    suspend fun save(user: PlatformUser): PlatformUser =
        withDbContext {
            userRepository.save(user)
        }
}