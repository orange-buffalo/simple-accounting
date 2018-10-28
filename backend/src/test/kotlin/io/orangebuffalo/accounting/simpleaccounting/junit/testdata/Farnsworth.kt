package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace

class Farnsworth : TestData {

    val himself = Prototypes.farnsworth()

    val workspace = Workspace(
        name = "Laboratory",
        owner = himself,
        taxEnabled = false,
        multiCurrencyEnabled = false,
        defaultCurrency = "USD"
    )

    override fun generateData() = listOf(himself, workspace)
}