package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
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
        category = slurmCategory,
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
        timeUploaded = MOCK_TIME
    )

    val secondSlurm = Expense(
        category = slurmCategory,
        datePaid = MOCK_DATE,
        timeRecorded = MOCK_TIME,
        currency = "ZZB",
        originalAmount = 5100,
        amountInDefaultCurrency = 510,
        actualAmountInDefaultCurrency = 460,
        reportedAmountInDefaultCurrency = 455,
        percentOnBusiness = 99,
        notes = "nice!",
        attachments = mutableListOf(slurmReceipt)
    )

    override fun generateData() = listOf(himself, workspace, slurmCategory, firstSlurm, slurmReceipt, secondSlurm)
}