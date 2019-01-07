package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_DATE
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_TIME

class Bender : TestData {

    val himself = Prototypes.bender()

    val planetExpress = Workspace(
        name = "Planet Express",
        owner = himself,
        taxEnabled = false,
        multiCurrencyEnabled = false,
        defaultCurrency = "USD"
    )

    val suicideBooth = Category(
        name = "Suicide booth",
        workspace = planetExpress,
        income = false,
        expense = true
    )

    val boothOne = Expense(
        workspace = suicideBooth.workspace,
        category = suicideBooth,
        title = "slow and horrible",
        datePaid = MOCK_DATE,
        timeRecorded = MOCK_TIME,
        currency = "USD",
        originalAmount = 25,
        amountInDefaultCurrency = 25,
        actualAmountInDefaultCurrency = 25,
        reportedAmountInDefaultCurrency = 25,
        percentOnBusiness = 100
    )

    val leagueOfRobots = Workspace(
        name = "League of Robots",
        owner = himself,
        taxEnabled = false,
        multiCurrencyEnabled = false,
        defaultCurrency = "USD"
    )

    override fun generateData() = listOf(himself, planetExpress, leagueOfRobots, suicideBooth, boothOne)
}