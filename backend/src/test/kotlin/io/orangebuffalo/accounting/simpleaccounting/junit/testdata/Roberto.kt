package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_TIME
import java.time.LocalDate

class Roberto : TestData {

    val himself = PlatformUser(
        userName = "Roberto",
        passwordHash = "knives!",
        isAdmin = false
    )

    val workspace = Workspace(
        name = "All around",
        owner = himself,
        taxEnabled = false,
        multiCurrencyEnabled = false,
        defaultCurrency = "AUD"
    )

    val irrelevantWorkspace = Workspace(
        name = "somewhere else",
        owner = himself,
        taxEnabled = false,
        multiCurrencyEnabled = false,
        defaultCurrency = "AUD"
    )

    val firstCategory = Category(
        name = "first",
        workspace = workspace,
        income = true,
        expense = true
    )

    val secondCategory = Category(
        name = "second",
        workspace = workspace,
        income = true,
        expense = true
    )

    val thirdCategory = Category(
        name = "third",
        workspace = workspace,
        income = true,
        expense = true
    )

    val irrelevantCategory = Category(
        name = "irrelevant",
        workspace = irrelevantWorkspace,
        income = true,
        expense = true
    )

    override fun generateData() = listOf(
        himself, workspace, irrelevantWorkspace, firstCategory, secondCategory, thirdCategory, irrelevantCategory,
        Expense(
            category = firstCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 4, 10),
            currency = "USD",
            reportedAmountInDefaultCurrency = 111,
            actualAmountInDefaultCurrency = 111,
            amountInDefaultCurrency = 111,
            originalAmount = 111,
            percentOnBusiness = 100
        ),
        Expense(
            category = firstCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 4, 9),
            currency = "USD",
            reportedAmountInDefaultCurrency = 110,
            actualAmountInDefaultCurrency = 110,
            amountInDefaultCurrency = 110,
            originalAmount = 110,
            percentOnBusiness = 100
        ),
        Expense(
            category = firstCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 10, 1),
            currency = "USD",
            reportedAmountInDefaultCurrency = 112,
            actualAmountInDefaultCurrency = 112,
            amountInDefaultCurrency = 112,
            originalAmount = 112,
            percentOnBusiness = 100
        ),
        Expense(
            category = firstCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 10, 2),
            currency = "USD",
            reportedAmountInDefaultCurrency = 113,
            actualAmountInDefaultCurrency = 113,
            amountInDefaultCurrency = 113,
            originalAmount = 113,
            percentOnBusiness = 100
        ),
        Expense(
            category = secondCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 6, 6),
            currency = "USD",
            reportedAmountInDefaultCurrency = 210,
            actualAmountInDefaultCurrency = 210,
            amountInDefaultCurrency = 210,
            originalAmount = 210,
            percentOnBusiness = 100
        ),
        Expense(
            category = secondCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 6, 7),
            currency = "USD",
            reportedAmountInDefaultCurrency = 211,
            actualAmountInDefaultCurrency = 211,
            amountInDefaultCurrency = 211,
            originalAmount = 211,
            percentOnBusiness = 100
        ),
        Expense(
            category = secondCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 6, 6),
            currency = "USD",
            reportedAmountInDefaultCurrency = 0,
            actualAmountInDefaultCurrency = 210,
            amountInDefaultCurrency = 210,
            originalAmount = 210,
            percentOnBusiness = 100
        ),
        Expense(
            category = secondCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 6, 6),
            currency = "USD",
            reportedAmountInDefaultCurrency = 0,
            actualAmountInDefaultCurrency = 0,
            amountInDefaultCurrency = 210,
            originalAmount = 210,
            percentOnBusiness = 100
        ),
        Expense(
            category = secondCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 6, 6),
            currency = "USD",
            reportedAmountInDefaultCurrency = 0,
            actualAmountInDefaultCurrency = 0,
            amountInDefaultCurrency = 0,
            originalAmount = 210,
            percentOnBusiness = 100
        ),
        Expense(
            category = irrelevantCategory,
            title = "expense",
            timeRecorded = MOCK_TIME,
            datePaid = LocalDate.of(3000, 6, 6),
            currency = "USD",
            reportedAmountInDefaultCurrency = 33,
            actualAmountInDefaultCurrency = 33,
            amountInDefaultCurrency = 33,
            originalAmount = 33,
            percentOnBusiness = 100
        ),
        Income(
            category = irrelevantCategory,
            timeRecorded = MOCK_TIME,
            title = "income",
            dateReceived = LocalDate.of(3010, 5, 23),
            currency = "USD",
            originalAmount = 177,
            amountInDefaultCurrency = 177,
            reportedAmountInDefaultCurrency = 177
        ),
        Income(
            category = firstCategory,
            timeRecorded = MOCK_TIME,
            title = "income",
            dateReceived = LocalDate.of(3010, 4, 20),
            currency = "USD",
            originalAmount = 166,
            amountInDefaultCurrency = 166,
            reportedAmountInDefaultCurrency = 166
        ),
        Income(
            category = firstCategory,
            timeRecorded = MOCK_TIME,
            title = "income",
            dateReceived = LocalDate.of(3010, 4, 21),
            currency = "USD",
            originalAmount = 167,
            amountInDefaultCurrency = 185,
            reportedAmountInDefaultCurrency = 167
        ),
        Income(
            category = firstCategory,
            timeRecorded = MOCK_TIME,
            title = "income",
            dateReceived = LocalDate.of(3010, 9, 15),
            currency = "USD",
            originalAmount = 168,
            amountInDefaultCurrency = 175,
            reportedAmountInDefaultCurrency = 168
        ),
        Income(
            category = firstCategory,
            timeRecorded = MOCK_TIME,
            title = "income",
            dateReceived = LocalDate.of(3010, 9, 16),
            currency = "USD",
            originalAmount = 177,
            amountInDefaultCurrency = 177,
            reportedAmountInDefaultCurrency = 177
        ),
        Income(
            category = secondCategory,
            timeRecorded = MOCK_TIME,
            title = "income",
            dateReceived = LocalDate.of(3010, 6, 1),
            currency = "USD",
            originalAmount = 233,
            amountInDefaultCurrency = 0,
            reportedAmountInDefaultCurrency = 0
        ),
        Income(
            category = secondCategory,
            timeRecorded = MOCK_TIME,
            title = "income",
            dateReceived = LocalDate.of(3010, 6, 1),
            currency = "USD",
            originalAmount = 233,
            amountInDefaultCurrency = 233,
            reportedAmountInDefaultCurrency = 0
        ),
        Income(
            category = secondCategory,
            timeRecorded = MOCK_TIME,
            title = "income",
            dateReceived = LocalDate.of(3010, 6, 1),
            currency = "USD",
            originalAmount = 233,
            amountInDefaultCurrency = 233,
            reportedAmountInDefaultCurrency = 233
        )
    )
}