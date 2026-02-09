package io.orangebuffalo.simpleaccounting.tests.infra.database

import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseAttachment
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentAttachment
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceAttachment
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDate

/**
 * To avoid failures on default values for columns with unique constraints.
 */
private var sequenceValue = 1

/**
 * API for creating entities in tests, first of all if not always - for tests preconditions.
 * It is rarely used standalone, but mostly indirectly via
 * [io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase.preconditions] or
 * [io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase.lazyPreconditions].
 */
class EntitiesFactory(private val infra: EntitiesFactoryInfra) {
    fun platformUser(
        userName: String = "Nibbler ${sequenceValue++}",
        passwordHash: String = "nopassword",
        isAdmin: Boolean = false,
        documentsStorage: String? = null,
        i18nSettings: I18nSettings = I18nSettings(locale = "en_AU", language = "en"),
        activated: Boolean = true,
    ) = PlatformUser(
        userName = userName,
        passwordHash = passwordHash,
        isAdmin = isAdmin,
        documentsStorage = documentsStorage,
        i18nSettings = i18nSettings,
        activated = activated
    ).save()

    fun userActivationToken(
        user: PlatformUser? = null,
        token: String = RandomStringUtils.randomAlphabetic(10),
        expiresAt: Instant = MOCK_TIME,
    ): UserActivationToken {
        val userId = if (user == null) platformUser().id else user.id
        return UserActivationToken(
            userId = userId!!,
            token = token,
            expiresAt = expiresAt,
        ).save()
    }

    /**
     * Regular user
     */
    fun fry() = platformUser(
        userName = "Fry",
        passwordHash = "qwertyHash",
        isAdmin = false,
        activated = true,
    )

    /**
     * Admin user, not workspaces expected to be mapped
     */
    fun farnsworth() = platformUser(
        userName = "Farnsworth",
        passwordHash = "scienceBasedHash",
        isAdmin = true,
        activated = true,
    )

    fun zoidberg() = platformUser(
        userName = "Zoidberg",
        passwordHash = "??",
        isAdmin = false,
        activated = true,
    )

    fun roberto() = platformUser(
        userName = "Roberto",
        passwordHash = "o_O",
        isAdmin = false,
        activated = true,
    )

    fun mafiaBot() = platformUser(
        userName = "MafiaBot",
        passwordHash = "$$$",
        isAdmin = false,
        activated = true,
    )

    fun bender() = platformUser(
        userName = "Bender",
        passwordHash = "011101010101101001",
        isAdmin = false,
        activated = true,
    )

    fun workspace(
        name: String = "Planet Express",
        owner: PlatformUser? = null,
        defaultCurrency: String = "USD"
    ): Workspace {
        val ownerId = if (owner == null) platformUser().id else owner.id
        return Workspace(
            name = name,
            ownerId = ownerId!!,
            defaultCurrency = defaultCurrency
        ).save()
    }

    fun workspaceAccessToken(
        workspace: Workspace? = null,
        timeCreated: Instant = MOCK_TIME,
        validTill: Instant = MOCK_TIME,
        revoked: Boolean = false,
        token: String = "token"
    ): WorkspaceAccessToken {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return WorkspaceAccessToken(
            workspaceId = workspaceId!!,
            timeCreated = timeCreated,
            validTill = validTill,
            revoked = revoked,
            token = token
        ).save()
    }

    fun document(
        name: String = "Slurm Receipt",
        workspace: Workspace? = null,
        timeUploaded: Instant = MOCK_TIME,
        storageId: String = TestDocumentsStorage.STORAGE_ID,
        storageLocation: String? = "test-location",
        sizeInBytes: Long? = null
    ): Document {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return Document(
            name = name,
            workspaceId = workspaceId!!,
            storageId = storageId,
            storageLocation = storageLocation,
            timeUploaded = timeUploaded,
            sizeInBytes = sizeInBytes
        ).save()
    }

    fun category(
        name: String = "Delivery",
        description: String? = null,
        workspace: Workspace? = null,
        income: Boolean = true,
        expense: Boolean = true
    ): Category {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return Category(
            name = name,
            workspaceId = workspaceId!!,
            income = income,
            expense = expense,
            description = description
        ).save()
    }

    fun generalTax(
        title: String = "Tax",
        rateInBps: Int = 10_00,
        description: String? = null,
        workspace: Workspace? = null,
    ): GeneralTax {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return GeneralTax(
            title = title,
            workspaceId = workspaceId!!,
            rateInBps = rateInBps,
            description = description
        ).save()
    }

    fun expense(
        category: Category? = null,
        workspace: Workspace? = null,
        title: String = "Expense",
        timeRecorded: Instant = MOCK_TIME,
        datePaid: LocalDate = MOCK_DATE,
        currency: String = "USD",
        originalAmount: Long = 100,
        convertedAmounts: AmountsInDefaultCurrency = AmountsInDefaultCurrency(100, 100),
        incomeTaxableAmounts: AmountsInDefaultCurrency = AmountsInDefaultCurrency(100, 100),
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean = false,
        attachments: Set<Document> = setOf(),
        percentOnBusiness: Int = 100,
        generalTax: GeneralTax? = null,
        generalTaxRateInBps: Int? = null,
        generalTaxAmount: Long? = null,
        notes: String? = null,
        status: ExpenseStatus = ExpenseStatus.FINALIZED
    ): Expense {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return Expense(
            workspaceId = workspaceId!!,
            categoryId = category?.id,
            title = title,
            datePaid = datePaid,
            timeRecorded = timeRecorded,
            currency = currency,
            originalAmount = originalAmount,
            convertedAmounts = convertedAmounts,
            incomeTaxableAmounts = incomeTaxableAmounts,
            useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
            percentOnBusiness = percentOnBusiness,
            generalTaxId = generalTax?.id,
            attachments = attachments.asSequence().map { ExpenseAttachment(it.id!!) }.toSet(),
            generalTaxAmount = generalTaxAmount,
            generalTaxRateInBps = generalTaxRateInBps,
            notes = notes,
            status = status
        ).save()
    }

    fun amountsInDefaultCurrency(
        amount: Long
    ): AmountsInDefaultCurrency = AmountsInDefaultCurrency(amount, amount)

    fun emptyAmountsInDefaultCurrency(): AmountsInDefaultCurrency = AmountsInDefaultCurrency(
        originalAmountInDefaultCurrency = null,
        adjustedAmountInDefaultCurrency = null
    )

    fun income(
        category: Category? = null,
        workspace: Workspace? = null,
        title: String = "Income",
        timeRecorded: Instant = MOCK_TIME,
        dateReceived: LocalDate = MOCK_DATE,
        currency: String = "USD",
        originalAmount: Long = 100,
        convertedAmounts: AmountsInDefaultCurrency = AmountsInDefaultCurrency(100, 100),
        incomeTaxableAmounts: AmountsInDefaultCurrency = AmountsInDefaultCurrency(100, 100),
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean = false,
        attachments: Set<Document> = setOf(),
        notes: String? = null,
        generalTax: GeneralTax? = null,
        generalTaxRateInBps: Int? = null,
        generalTaxAmount: Long? = null,
        status: IncomeStatus = IncomeStatus.FINALIZED,
        linkedInvoice: Invoice? = null
    ): Income {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return Income(
            categoryId = category?.id,
            workspaceId = workspaceId!!,
            generalTaxAmount = generalTaxAmount,
            convertedAmounts = convertedAmounts,
            useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
            incomeTaxableAmounts = incomeTaxableAmounts,
            status = status,
            generalTaxId = generalTax?.id,
            notes = notes,
            generalTaxRateInBps = generalTaxRateInBps,
            attachments = attachments.asSequence().map { document -> IncomeAttachment(document.id!!) }.toSet(),
            currency = currency,
            dateReceived = dateReceived,
            originalAmount = originalAmount,
            timeRecorded = timeRecorded,
            title = title,
            linkedInvoiceId = linkedInvoice?.id
        ).save()
    }

    fun customer(
        name: String = "customer",
        workspace: Workspace? = null,
    ): Customer {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return Customer(
            name = name,
            workspaceId = workspaceId!!,
        ).save()
    }

    fun invoice(
        customer: Customer? = null,
        title: String = "invoice",
        timeRecorded: Instant = MOCK_TIME,
        dateIssued: LocalDate = MOCK_DATE,
        dateSent: LocalDate? = null,
        datePaid: LocalDate? = null,
        timeCancelled: Instant? = null,
        dueDate: LocalDate = MOCK_DATE,
        currency: String = "USD",
        amount: Long = 100,
        attachments: Set<Document> = setOf(),
        notes: String? = null,
        generalTax: GeneralTax? = null,
        status: InvoiceStatus = InvoiceStatus.DRAFT
    ): Invoice {
        val customerId = if (customer == null) customer().id else customer.id
        return Invoice(
            customerId = customerId!!,
            title = title,
            timeRecorded = timeRecorded,
            dateIssued = dateIssued,
            dateSent = dateSent,
            datePaid = datePaid,
            timeCancelled = timeCancelled,
            dueDate = dueDate,
            currency = currency,
            amount = amount,
            attachments = attachments.asSequence().map { document ->
                InvoiceAttachment(document.id!!)
            }.toSet(),
            notes = notes,
            generalTaxId = generalTax?.id,
            status = status
        ).save()
    }

    fun incomeTaxPayment(
        workspace: Workspace? = null,
        timeRecorded: Instant = MOCK_TIME,
        datePaid: LocalDate = MOCK_DATE,
        reportingDate: LocalDate = MOCK_DATE,
        amount: Long = 100,
        title: String = "Tax Payment",
        attachments: Set<Document> = setOf(),
        notes: String? = null
    ): IncomeTaxPayment {
        val workspaceId = if (workspace == null) workspace().id else workspace.id
        return IncomeTaxPayment(
            workspaceId = workspaceId!!,
            timeRecorded = timeRecorded,
            datePaid = datePaid,
            reportingDate = reportingDate,
            amount = amount,
            title = title,
            attachments = attachments.asSequence().map { IncomeTaxPaymentAttachment(it.id!!) }.toSet(),
            notes = notes
        ).save()
    }

    fun <T : Any> save(vararg entities: T) = entities.forEach { infra.save(it) }

    fun <T : Any> T.save(): T = infra.save(this)

    /**
     * Most of the functionality requires workspace to be setup for a user.
     * This shortcut allows to configure this as part of the preconditions
     * with minimum overheads.
     */
    fun PlatformUser.withWorkspace() : PlatformUser = this.also {
        workspace(owner = this)
    }
}

/**
 * Infrastructure for creating test entities.
 */
class EntitiesFactoryInfra(
    platformTransactionManager: PlatformTransactionManager,
    private val jdbcAggregateTemplate: JdbcAggregateTemplate,
) {

    private val transactionTemplate = TransactionTemplate().apply {
        transactionManager = platformTransactionManager
        propagationBehavior = TransactionTemplate.PROPAGATION_REQUIRES_NEW
    }

    fun <T : Any> save(entity: T): T {
        return transactionTemplate.execute {
            jdbcAggregateTemplate.save(entity)
        }!!
    }
}
