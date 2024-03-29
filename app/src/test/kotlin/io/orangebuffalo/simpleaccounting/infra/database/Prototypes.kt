package io.orangebuffalo.simpleaccounting.infra.database

import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.domain.documents.Document
import io.orangebuffalo.simpleaccounting.domain.invoices.Invoice
import io.orangebuffalo.simpleaccounting.domain.invoices.InvoiceAttachment
import io.orangebuffalo.simpleaccounting.domain.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import java.time.Instant
import java.time.LocalDate

// JavaScript min number value is -9007199254740991, so we need to reduce the magnitude of ids to avoid overflows
internal var currentEntityId: Long = Long.MIN_VALUE / 10_000

class Prototypes {
    companion object {
        /**
         * Regular user
         */
        fun fry() = platformUser(
            userName = "Fry",
            passwordHash = "qwertyHash",
            isAdmin = false
        )

        /**
         * Admin user, not workspaces expected to be mapped
         */
        fun farnsworth() = platformUser(
            userName = "Farnsworth",
            passwordHash = "scienceBasedHash",
            isAdmin = true
        )

        fun zoidberg() = platformUser(
            userName = "Zoidberg",
            passwordHash = "??",
            isAdmin = false
        )

        fun roberto() = platformUser(
            userName = "Roberto",
            passwordHash = "o_O",
            isAdmin = false
        )

        fun mafiaBot() = platformUser(
            userName = "MafiaBot",
            passwordHash = "$$$",
            isAdmin = false
        )

        fun bender() = platformUser(
            userName = "Bender",
            passwordHash = "011101010101101001",
            isAdmin = false
        )

        fun platformUser(
            userName: String = "Farnsworth",
            passwordHash: String = "nopassword",
            isAdmin: Boolean = false,
            documentsStorage: String? = null,
            i18nSettings: I18nSettings = I18nSettings(locale = "en_AU", language = "en")
        ) = PlatformUser(
            userName = userName,
            passwordHash = passwordHash,
            isAdmin = isAdmin,
            documentsStorage = documentsStorage,
            i18nSettings = i18nSettings
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun workspace(
            name: String = "Planet Express",
            owner: PlatformUser = platformUser(),
            taxEnabled: Boolean = true,
            multiCurrencyEnabled: Boolean = true,
            defaultCurrency: String = "USD"
        ) = Workspace(
            name = name,
            ownerId = owner.id!!,
            taxEnabled = taxEnabled,
            multiCurrencyEnabled = multiCurrencyEnabled,
            defaultCurrency = defaultCurrency
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun generalTax(
            title: String = "Tax",
            rateInBps: Int = 10_00,
            description: String? = null,
            workspace: Workspace = workspace()
        ) = GeneralTax(
            title = title,
            workspaceId = workspace.id!!,
            rateInBps = rateInBps,
            description = description
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun category(
            name: String = "Delivery",
            description: String? = null,
            workspace: Workspace = workspace(),
            income: Boolean = true,
            expense: Boolean = true
        ) = Category(
            name = name,
            workspaceId = workspace.id!!,
            income = income,
            expense = expense,
            description = description
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun expense(
            category: Category? = null,
            workspace: Workspace = workspace(),
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
        ) = Expense(
            workspaceId = workspace.id!!,
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
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun income(
            category: Category? = null,
            workspace: Workspace = workspace(),
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
        ) = Income(
            categoryId = category?.id,
            workspaceId = workspace.id!!,
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
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun document(
            name: String = "Slurm Receipt",
            workspace: Workspace = workspace(),
            timeUploaded: Instant = MOCK_TIME,
            storageId: String = "test-storage",
            storageLocation: String? = "test-location",
            sizeInBytes: Long? = null
        ): Document = Document(
            name = name,
            workspaceId = workspace.id!!,
            storageId = storageId,
            storageLocation = storageLocation,
            timeUploaded = timeUploaded,
            sizeInBytes = sizeInBytes
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun incomeTaxPayment(
            workspace: Workspace = workspace(),
            timeRecorded: Instant = MOCK_TIME,
            datePaid: LocalDate = MOCK_DATE,
            reportingDate: LocalDate = MOCK_DATE,
            amount: Long = 100,
            title: String = "Tax Payment",
            attachments: Set<Document> = setOf(),
            notes: String? = null
        ): IncomeTaxPayment = IncomeTaxPayment(
            workspaceId = workspace.id!!,
            timeRecorded = timeRecorded,
            datePaid = datePaid,
            reportingDate = reportingDate,
            amount = amount,
            title = title,
            attachments = attachments.asSequence().map { IncomeTaxPaymentAttachment(it.id!!) }.toSet(),
            notes = notes
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun customer(
            name: String = "customer",
            workspace: Workspace = workspace()
        ): Customer = Customer(
            name = name,
            workspaceId = workspace.id!!
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun invoice(
            customer: Customer = customer(),
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
        ): Invoice = Invoice(
            customerId = customer.id!!,
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
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun workspaceAccessToken(
            workspace: Workspace = workspace(),
            timeCreated: Instant = MOCK_TIME,
            validTill: Instant = MOCK_TIME,
            revoked: Boolean = false,
            token: String = "token"
        ): WorkspaceAccessToken = WorkspaceAccessToken(
            workspaceId = workspace.id!!,
            timeCreated = timeCreated,
            validTill = validTill,
            revoked = revoked,
            token = token
        ).apply {
            id = currentEntityId++
            version = 0
        }

        fun amountsInDefaultCurrency(
            amount: Long
        ): AmountsInDefaultCurrency = AmountsInDefaultCurrency(amount, amount)

        fun emptyAmountsInDefaultCurrency(): AmountsInDefaultCurrency = AmountsInDefaultCurrency(
            originalAmountInDefaultCurrency = null,
            adjustedAmountInDefaultCurrency = null
        )
    }
}
