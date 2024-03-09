package io.orangebuffalo.simpleaccounting.domain.users

import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.security.ensureRegularUserPrincipal
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

@Service
class PlatformUserService(
    private val userRepository: PlatformUserRepository,
    private val userActivationTokenRepository: UserActivationTokenRepository,
    private val userManagementProperties: UserManagementProperties,
    private val timeService: TimeService,
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

    suspend fun createUser(
        userName: String,
        isAdmin: Boolean,
    ): PlatformUser {
        val user = save(
            PlatformUser(
                userName = userName,
                passwordHash = RandomStringUtils.random(100),
                isAdmin = isAdmin,
                i18nSettings = I18nSettings(locale = "en_AU", language = "en"),
                activated = false
            )
        )
        withDbContext {
            userActivationTokenRepository.save(
                UserActivationToken(
                    userId = user.id!!,
                    token = RandomStringUtils.randomAscii(100),
                    expiresAt = timeService.currentTime().plus(
                        userManagementProperties.activation.tokenTtlInHours.toLong(),
                        ChronoUnit.HOURS,
                    )
                )
            )
        }
        return user
    }

    suspend fun getUserByUserId(userId: Long): PlatformUser = withDbContext {
        userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User $userId is not found") }
    }
}
