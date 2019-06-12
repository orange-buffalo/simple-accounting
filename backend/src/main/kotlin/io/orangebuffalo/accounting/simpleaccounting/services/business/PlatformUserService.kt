package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.integration.getCurrentPrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContextAsync
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.PlatformUserRepository
import kotlinx.coroutines.Deferred
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PlatformUserService(
    private val userRepository: PlatformUserRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend fun getCurrentUserAsync(): Deferred<PlatformUser> =
        withDbContextAsync {
            userRepository.findByUserName(getCurrentPrincipal().username)
                ?: throw IllegalStateException("Current principal is not resolved to a user")
        }

    suspend fun getCurrentUser(): PlatformUser =
        withDbContext {
            userRepository.findByUserName(getCurrentPrincipal().username)
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