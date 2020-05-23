package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.repos.PlatformUserRepository
import io.orangebuffalo.simpleaccounting.services.security.ensureRegularUserPrincipal
import org.springframework.stereotype.Service

@Service
class PlatformUserService(
    private val userRepository: PlatformUserRepository
) {

    suspend fun getCurrentUser(): PlatformUser = withDbContext {
        userRepository.findByUserName(ensureRegularUserPrincipal().userName)
            ?: throw IllegalStateException("Current principal is not resolved to a user")
    }

    suspend fun getUserByUserName(userName: String): PlatformUser? = withDbContext {
        userRepository.findByUserName(userName)
    }

    suspend fun save(user: PlatformUser): PlatformUser = withDbContext {
        userRepository.save(user)
    }

    suspend fun getUserByUserId(userId: Long): PlatformUser = withDbContext {
        userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User $userId is not found") }
    }
}
