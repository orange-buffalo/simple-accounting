package io.orangebuffalo.accounting.simpleaccounting.support

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.transaction.Transactional

/**
 * Generates data for CI Tests during application startup.
 */
@Component
@Profile("ci-tests")
class CiTestsProfileData(private val entityManager: EntityManager) {

    @EventListener
    @Transactional
    fun createCiTestsData(event: ContextRefreshedEvent) {
       entityManager.persist(PlatformUser(
           userName = "Fry",
           passwordHash = "{noop}password",
           isAdmin = false
       ))
    }
}