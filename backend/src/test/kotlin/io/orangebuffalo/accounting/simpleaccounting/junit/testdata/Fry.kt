package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace

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

    override fun generateData() = listOf(himself, workspace, slurmCategory)
}