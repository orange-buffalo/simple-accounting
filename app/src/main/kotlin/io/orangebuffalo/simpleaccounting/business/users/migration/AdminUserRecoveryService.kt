package io.orangebuffalo.simpleaccounting.business.users.migration

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * Ensures that system has at least one admin user. It is expected to be effectively executed
 * only once, since after an admin user is created, at least one user will be kept in the system
 * (e.g. user removal will fail for the last admin user).
 *
 * It is important to catch the username / password from the logs and change the password as
 * soon as possible to avoid any security risks.
 */
@Service
class AdminUserRecoveryService(private val aggregateTemplate: JdbcAggregateTemplate) {

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun recoverAdminUser() {
        log.info { "Starting admin user recovery" }
        val adminUser = aggregateTemplate
            .findAll(PlatformUser::class.java)
            .find { it.isAdmin }
        if (adminUser == null) {
            val password = RandomStringUtils.randomAlphanumeric(15)
            val userName = "admin"
            aggregateTemplate.insert(
                PlatformUser(
                    userName = userName,
                    passwordHash = "{noop}$password",
                    isAdmin = true,
                    activated = true,
                )
            )
            log.warn {
                "Application database does not contain any admin users. " +
                        "Created a new user with login '$userName' and password '$password'. " +
                        "It is highly recommended to change the generated password."
            }
        } else {
            log.info { "Admin user found, nothing to do" }
        }
    }
}
