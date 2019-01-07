package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_DATE
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_TIME

class Fry : TestData {

    val himself = Prototypes.fry()

    val workspace = Workspace(
        name = "Property of Philip J. Fry",
        owner = himself,
        taxEnabled = false,
        multiCurrencyEnabled = false,
        defaultCurrency = "USD"
    )

    val slurmCategory = Category(
        name = "for Slurm",
        description = "Only for the best drink ever",
        workspace = workspace,
        income = true,
        expense = true
    )

    val firstSlurm = Expense(
        workspace = slurmCategory.workspace,
        category = slurmCategory,
        title = "best ever slurm",
        datePaid = MOCK_DATE,
        timeRecorded = MOCK_TIME,
        currency = "THF",
        originalAmount = 5000,
        amountInDefaultCurrency = 500,
        actualAmountInDefaultCurrency = 450,
        reportedAmountInDefaultCurrency = 450,
        percentOnBusiness = 100
    )

    val slurmReceipt = Document(
        name = "slurm",
        workspace = workspace,
        storageProviderId = "local-fs",
        storageProviderLocation = "lost",
        timeUploaded = MOCK_TIME,
        sizeInBytes = 30
    )

    val coffeeReceipt = Document(
        name = "100_cups.pdf",
        workspace = workspace,
        storageProviderId = "local-fs",
        storageProviderLocation = "coffee.pdf",
        timeUploaded = MOCK_TIME,
        sizeInBytes = 42
    )

    val cheesePizzaAndALargeSodaReceipt = Document(
        name = "unknown",
        workspace = workspace,
        storageProviderId = "local-fs",
        storageProviderLocation = "lost",
        timeUploaded = MOCK_TIME,
        notes = "Panucci's Pizza",
        sizeInBytes = null
    )

    val secondSlurm = Expense(
        workspace = slurmCategory.workspace,
        category = slurmCategory,
        title = "another great slurm",
        datePaid = MOCK_DATE,
        timeRecorded = MOCK_TIME,
        currency = "ZZB",
        originalAmount = 5100,
        amountInDefaultCurrency = 510,
        actualAmountInDefaultCurrency = 460,
        reportedAmountInDefaultCurrency = 455,
        percentOnBusiness = 99,
        notes = "nice!",
        attachments = setOf(slurmReceipt)
    )

    val refreshToken = RefreshToken(
        user = himself,
        token = "42:34jFbT3h2=",
        expirationTime = MOCK_TIME
    )

    override fun generateData() = listOf(
        himself,
        workspace,
        slurmCategory,
        firstSlurm,
        slurmReceipt,
        secondSlurm,
        coffeeReceipt,
        cheesePizzaAndALargeSodaReceipt,
        refreshToken
    )
}