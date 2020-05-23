package io.orangebuffalo.simpleaccounting.support

import io.orangebuffalo.simpleaccounting.services.persistence.entities.I18nSettings
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Generates data for CI Tests during application startup.
 */
@Component
@Profile("ci-tests")
class CiTestsProfileData(private val jdbcAggregateTemplate: JdbcAggregateTemplate) {

    @EventListener
    @Transactional
    fun createCiTestsData(event: ContextRefreshedEvent) {
        jdbcAggregateTemplate.insert(
            PlatformUser(
                userName = "Fry",
                passwordHash = "{noop}password",
                isAdmin = false,
                i18nSettings = I18nSettings(locale = "en_AU", language = "en")
            )
        )
    }
}
