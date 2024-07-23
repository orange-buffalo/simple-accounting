package io.orangebuffalo.simpleaccounting.support

import io.orangebuffalo.simpleaccounting.domain.customers.Customer
import io.orangebuffalo.simpleaccounting.domain.invoices.Invoice
import io.orangebuffalo.simpleaccounting.domain.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.domain.users.I18nSettings
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.domain.workspaces.Workspace
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

/**
 * Generates data for end-to-end Tests during application startup.
 */
@Component
@Profile("ci-tests")
class CiTestsProfileData(private val jdbcAggregateTemplate: JdbcAggregateTemplate) {

    @EventListener
    @Transactional
    fun createCiTestsData(event: ContextRefreshedEvent) {
        val fry = jdbcAggregateTemplate.insert(
            PlatformUser(
                userName = "Fry",
                passwordHash = "{noop}password",
                isAdmin = false,
                i18nSettings = I18nSettings(locale = "en_AU", language = "en"),
                activated = true
            )
        )

        val planetExpress = jdbcAggregateTemplate.insert(
            Workspace(
                ownerId = fry.id!!,
                defaultCurrency = "AUD",
                multiCurrencyEnabled = true,
                name = "Planet Express",
                taxEnabled = true,
            )
        )

        val lunaPark = jdbcAggregateTemplate.insert(
            Customer(
                workspaceId = planetExpress.id!!,
                name = "Luna Park"
            )
        )

        jdbcAggregateTemplate.insert(
            Invoice(
                customerId = lunaPark.id!!,
                status = InvoiceStatus.DRAFT,
                dueDate = LocalDate.of(3000, 4, 10),
                dateSent = null,
                datePaid = null,
                timeCancelled = null,
                amount = 56_890_00,
                attachments = emptySet(),
                currency = "AUD",
                dateIssued = LocalDate.of(3000, 2, 10),
                generalTaxId = null,
                notes = null,
                timeRecorded = Instant.now(),
                title = "Stuffed Toys"
            )
        )
    }
}
