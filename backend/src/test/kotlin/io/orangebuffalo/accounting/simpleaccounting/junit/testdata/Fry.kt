package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_TIME
import java.time.ZonedDateTime

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
        //TODO
        datePaid = ZonedDateTime.now(),
        dateRecorded = ZonedDateTime.now(),
        currency = "THF",
        originalAmount = 5000,
        amountInDefaultCurrency = 500,
        actualAmountInDefaultCurrency = 450,
        percentOnBusinessInBps = 10000
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
        //TODO
        datePaid = ZonedDateTime.now(),
        dateRecorded = ZonedDateTime.now(),
        currency = "ZZB",
        originalAmount = 5100,
        amountInDefaultCurrency = 510,
        actualAmountInDefaultCurrency = 460,
        percentOnBusinessInBps = 9900,
        notes = "nice!",
        attachments = mutableListOf(slurmReceipt)
    )

    override fun generateData() = listOf(himself, workspace, slurmCategory, firstSlurm, slurmReceipt, secondSlurm)
}