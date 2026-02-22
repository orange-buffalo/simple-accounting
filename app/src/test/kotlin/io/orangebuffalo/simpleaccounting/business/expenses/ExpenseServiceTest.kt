package io.orangebuffalo.simpleaccounting.business.expenses

import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
@DisplayName("ExpenseService")
internal class ExpenseServiceTest(
    @Autowired private val expenseService: ExpenseService
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val nonDefaultCurrency = "CC2"
            val fry = fry()
            val workspace = workspace(
                owner = fry,
                defaultCurrency = "CC1"
            )
            val generalTaxFromWorkspace = generalTax(
                workspace = workspace,
                rateInBps = 10_00
            )
        }
    }

    @Test
    @WithMockFryUser
    fun `should reset the values for default currency`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(33),
                incomeTaxableAmounts = AmountsInDefaultCurrency(33),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.workspace.defaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(45),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(45),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = ExpenseStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate percent on business for default currency`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(33),
                incomeTaxableAmounts = AmountsInDefaultCurrency(33),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.workspace.defaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 10,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 45,
                adjustedAmountInDefaultCurrency = 5
            ),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 45,
                adjustedAmountInDefaultCurrency = 5
            ),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = ExpenseStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax on default currency`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 4500,
                convertedAmounts = AmountsInDefaultCurrency(33),
                incomeTaxableAmounts = AmountsInDefaultCurrency(33),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.workspace.defaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 4500,
            expectedConvertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4500,
                adjustedAmountInDefaultCurrency = 4091 // base for 10% added tax
            ),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4500,
                adjustedAmountInDefaultCurrency = 4091
            ),
            expectedGeneralTax = preconditions.generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 409, // original amount - reported amount
            expectedGeneralTaxRateInBps = 10_00,
            expectedStatus = ExpenseStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax on default currency with percent on business`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 450,
                convertedAmounts = AmountsInDefaultCurrency(33),
                incomeTaxableAmounts = AmountsInDefaultCurrency(33),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.workspace.defaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 90,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 450,
            expectedConvertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 450,
                adjustedAmountInDefaultCurrency = 368 // 90% of 450 = 405, base for 10% added tax is 368
            ),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 450,
                adjustedAmountInDefaultCurrency = 368
            ),
            expectedGeneralTax = preconditions.generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 37, // business spent amount 405 - base amount 368
            expectedGeneralTaxRateInBps = 10_00,
            expectedStatus = ExpenseStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )
    }

    @Test
    @WithMockFryUser
    fun `should keep amounts as null if not yet converted`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(null),
                incomeTaxableAmounts = AmountsInDefaultCurrency(null),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                workspaceId = preconditions.workspace.id!!,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(null),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(null),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = ExpenseStatus.PENDING_CONVERSION,
            expectedUseDifferentExchangeRates = true
        )
    }

    @Test
    @WithMockFryUser
    fun `should set amount to null if not yet converted and same rate used for conversion`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 42
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(44),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                status = ExpenseStatus.FINALIZED,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(null),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(null),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = ExpenseStatus.PENDING_CONVERSION
        )
    }

    @Test
    @WithMockFryUser
    fun `should keep income taxable amount if not yet converted and different rate used for conversion`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 42
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(44),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                status = ExpenseStatus.FINALIZED,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(null),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(44),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.PENDING_CONVERSION
        )
    }

    @Test
    @WithMockFryUser
    fun `should propagate converted amount if same rate is used`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.PENDING_CONVERSION,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(30),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = ExpenseStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should keep income taxable amount if different rate is used`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(100),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should keep income taxable amount empty if different rate is used`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 300
                ),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(null),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
        )
    }

    @Test
    @WithMockFryUser
    fun `should use converted amounts to calculate percent on business`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(41),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                percentOnBusiness = 90,
                status = ExpenseStatus.PENDING_CONVERSION,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 30,
                adjustedAmountInDefaultCurrency = 27 // 90% of amount of 30
            ),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 41,
                adjustedAmountInDefaultCurrency = 37 // 90% of actual amount 41
            ),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax based on income taxable amount if different rate is used`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(41),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 30,
                adjustedAmountInDefaultCurrency = 27 // base for 10% added tax to get 30
            ),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 41,
                adjustedAmountInDefaultCurrency = 37 // base for 10% added tax to get 41
            ),
            expectedGeneralTax = preconditions.generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 4, // actual amount 41 - base amount 37
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax bases on converted amount if same rate is used`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(41),
                incomeTaxableAmounts = AmountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.PENDING_CONVERSION,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 41,
                adjustedAmountInDefaultCurrency = 37 // base for 10% added tax to get 41
            ),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 41,
                adjustedAmountInDefaultCurrency = 37
            ),
            expectedGeneralTax = preconditions.generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 4, // actual amount 41 - base amount 37
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = ExpenseStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax based on income taxable amount if percent on business provided and different rate used`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 4500,
                convertedAmounts = AmountsInDefaultCurrency(4000),
                incomeTaxableAmounts = AmountsInDefaultCurrency(4100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 80,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 4500,
            expectedConvertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4000,
                adjustedAmountInDefaultCurrency = 2909  // 80% of 4000 = 3200, base for 10% added tax for 3200 is 2909
            ),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4100,
                adjustedAmountInDefaultCurrency = 2982 // 80% of 4100 = 3280, base for 10% added tax for 3280 is 2982
            ),
            expectedGeneralTax = preconditions.generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 298, // 3280 of taxable amount - base amount of 2982
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax based on converted amount if percent on business provided and same rate used`() {
        executeSaveExpenseAndAssert(
            expense = Expense(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 4500,
                convertedAmounts = AmountsInDefaultCurrency(4000),
                incomeTaxableAmounts = AmountsInDefaultCurrency(4100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                percentOnBusiness = 80,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.PENDING_CONVERSION,
                datePaid = MOCK_DATE,
                categoryId = null,
                notes = null,
                title = "Expense",
                timeRecorded = MOCK_TIME,
                attachments = setOf(),
            ),
            expectedOriginalAmount = 4500,
            expectedConvertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4000,
                adjustedAmountInDefaultCurrency = 2909  // 80% of 4000 = 3200, base for 10% added tax for 3200 is 2909
            ),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4000,
                adjustedAmountInDefaultCurrency = 2909
            ),
            expectedGeneralTax = preconditions.generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 291, // based on converted amount of 3200
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = ExpenseStatus.FINALIZED
        )
    }

    private fun executeSaveExpenseAndAssert(
        expense: Expense,
        expectedOriginalAmount: Long,
        expectedConvertedAmounts: AmountsInDefaultCurrency,
        expectedIncomeTaxableAmounts: AmountsInDefaultCurrency,
        expectedGeneralTax: GeneralTax?,
        expectedGeneralTaxRateInBps: Int?,
        expectedGeneralTaxAmount: Long?,
        expectedStatus: ExpenseStatus,
        expectedUseDifferentExchangeRates: Boolean
    ) {
        val actualExpense = runBlocking {
            expenseService.saveExpense(expense)
        }

        assertThat(actualExpense.originalAmount).isEqualTo(expectedOriginalAmount)
        assertThat(actualExpense.status).isEqualTo(expectedStatus)
        assertThat(actualExpense.convertedAmounts).isEqualTo(expectedConvertedAmounts)
        assertThat(actualExpense.incomeTaxableAmounts).isEqualTo(expectedIncomeTaxableAmounts)
        assertThat(actualExpense.generalTaxId).isEqualTo(expectedGeneralTax?.id)
        assertThat(actualExpense.generalTaxAmount).isEqualTo(expectedGeneralTaxAmount)
        assertThat(actualExpense.generalTaxRateInBps).isEqualTo(expectedGeneralTaxRateInBps)
        assertThat(actualExpense.useDifferentExchangeRateForIncomeTaxPurposes)
            .isEqualTo(expectedUseDifferentExchangeRates)
    }
}
