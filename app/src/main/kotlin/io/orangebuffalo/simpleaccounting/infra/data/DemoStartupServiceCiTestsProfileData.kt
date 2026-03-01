package io.orangebuffalo.simpleaccounting.infra.data

import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Generates data for the demo runtime during application startup.
 */
@Component
@ConditionalOnProperty("sa.demo.enabled", havingValue = "true", matchIfMissing = false)
class DemoStartupServiceCiTestsProfileData(private val jdbcAggregateTemplate: JdbcAggregateTemplate) {

    @EventListener
    @Transactional
    fun createDemoData(event: ContextRefreshedEvent) {
        log.warn { "ATTENTION: Running application in demo mode" }

        val fry = PlatformUser(
            userName = "Fry",
            passwordHash = "{noop}password",
            isAdmin = false,
            i18nSettings = I18nSettings(locale = "en", language = "en"),
            activated = true
        ).also { jdbcAggregateTemplate.insert(it) }
        PlatformUser(
            userName = "Hermes",
            passwordHash = "{noop}password",
            isAdmin = true,
            i18nSettings = I18nSettings(locale = "en", language = "en"),
            activated = true
        ).also { jdbcAggregateTemplate.insert(it) }

        val planetExpressWorkspace = Workspace(
            name = "Planet Express",
            ownerId = fry.id!!,
            defaultCurrency = "USD"
        ).also { jdbcAggregateTemplate.insert(it) }

        val delivery = Category(
            name = "Delivery",
            workspaceId = planetExpressWorkspace.id!!,
            income = true,
            expense = false
        ).also { jdbcAggregateTemplate.insert(it) }
        val shipMaintenance = Category(
            name = "Ship Maintenance",
            workspaceId = planetExpressWorkspace.id!!,
            income = false,
            expense = true
        ).also { jdbcAggregateTemplate.insert(it) }

        Customer(name = "MomCorp", workspaceId = planetExpressWorkspace.id!!)
            .also { jdbcAggregateTemplate.insert(it) }
        val omicronians = Customer(name = "Omicronians", workspaceId = planetExpressWorkspace.id!!)
            .also { jdbcAggregateTemplate.insert(it) }

        val spaceVAT = GeneralTax(title = "Space VAT", rateInBps = 1900, workspaceId = planetExpressWorkspace.id!!)
            .also { jdbcAggregateTemplate.insert(it) }

        Expense(
            categoryId = shipMaintenance.id,
            workspaceId = planetExpressWorkspace.id!!,
            title = "Dark Matter Fuel",
            datePaid = LocalDate.now(),
            currency = "USD",
            originalAmount = 3000,
            convertedAmounts = AmountsInDefaultCurrency(3000, 2850),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            incomeTaxableAmounts = AmountsInDefaultCurrency(3000, 2850),
            status = ExpenseStatus.FINALIZED,
            percentOnBusiness = 100,
            generalTaxId = spaceVAT.id,
        ).also { jdbcAggregateTemplate.insert(it) }
        Expense(
            categoryId = shipMaintenance.id,
            workspaceId = planetExpressWorkspace.id!!,
            title = "Slurm Beverages for Crew",
            datePaid = LocalDate.now(),
            currency = "USD",
            originalAmount = 50,
            convertedAmounts = AmountsInDefaultCurrency(50, 48),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            incomeTaxableAmounts = AmountsInDefaultCurrency(50, 48),
            status = ExpenseStatus.FINALIZED,
            percentOnBusiness = 100,
            generalTaxId = null,
        ).also { jdbcAggregateTemplate.insert(it) }

        Income(
            categoryId = delivery.id,
            workspaceId = planetExpressWorkspace.id!!,
            title = "Delivery to MomCorp",
            dateReceived = LocalDate.now(),
            currency = "USD",
            originalAmount = 7000,
            convertedAmounts = AmountsInDefaultCurrency(7000, 6650),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            incomeTaxableAmounts = AmountsInDefaultCurrency(7000, 6650),
            status = IncomeStatus.FINALIZED,
            generalTaxId = spaceVAT.id,
        ).also { jdbcAggregateTemplate.insert(it) }
        Income(
            categoryId = delivery.id,
            workspaceId = planetExpressWorkspace.id!!,
            title = "Ransom from Omicronians",
            dateReceived = LocalDate.now(),
            currency = "USD",
            originalAmount = 2000,
            convertedAmounts = AmountsInDefaultCurrency(2000, 1900),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            incomeTaxableAmounts = AmountsInDefaultCurrency(2000, 1900),
            status = IncomeStatus.FINALIZED,
            generalTaxId = null,
        ).also { jdbcAggregateTemplate.insert(it) }

        IncomeTaxPayment(
            workspaceId = planetExpressWorkspace.id!!,
            datePaid = LocalDate.now(),
            reportingDate = LocalDate.now(),
            amount = 1200,
            title = "Q1 Space Income Tax"
        ).also { jdbcAggregateTemplate.insert(it) }

        Invoice(
            customerId = omicronians.id!!,
            title = "Delivery to Omicron Persei 8",
            dateIssued = LocalDate.now().plusDays(2),
            dueDate = LocalDate.now().plusDays(30),
            currency = "USD",
            amount = 3000,
            status = InvoiceStatus.DRAFT
        ).also { jdbcAggregateTemplate.insert(it) }
    }
}
