package io.orangebuffalo.simpleaccounting.services.business

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.services.persistence.entities.LegacyAmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.simpleaccounting.services.persistence.entities.ExpenseStatus
import io.orangebuffalo.simpleaccounting.services.persistence.entities.GeneralTax
import io.orangebuffalo.simpleaccounting.services.persistence.repos.ExpenseRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import javax.persistence.EntityManagerFactory

@ExtendWith(MockitoExtension::class)
@DisplayName("ExpenseService")
internal class ExpenseServiceTest {

    @field:Mock
    private lateinit var expenseRepository: ExpenseRepository

    @field:Mock
    private lateinit var entityManagerFactory: EntityManagerFactory

    @field:InjectMocks
    private lateinit var expenseService: ExpenseService

    private val workspace = Prototypes.workspace()
    private val generalTaxFromWorkspace = Prototypes.generalTax(workspace = workspace, rateInBps = 10_00)

    @Test
    fun `should fail on expense validation if expense is in default currency and converted amount differs`() {
        val expense = Prototypes.expense(
            currency = workspace.defaultCurrency,
            originalAmount = 200,
            convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(199)
        )
        assertThatThrownBy { expenseService.validateExpenseConsistency(expense) }
            .hasMessage("Inconsistent expense: converted amount does not match original for default currency")
    }

    @Test
    fun `should fail on expense validation if expense is in default currency and income taxable amount differs`() {
        val expense = Prototypes.expense(
            currency = workspace.defaultCurrency,
            originalAmount = 200,
            convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(200),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(199)
        )
        assertThatThrownBy { expenseService.validateExpenseConsistency(expense) }
            .hasMessage("Inconsistent expense: income taxable amount does not match original for default currency")
    }

    @Test
    fun `should fail on expense validation if same conversion rate is used but amounts differ`() {
        val expense = Prototypes.expense(
            currency = "--${workspace.defaultCurrency}--",
            originalAmount = 200,
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(200),
            incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(199)
        )
        assertThatThrownBy { expenseService.validateExpenseConsistency(expense) }
            .hasMessage("Inconsistent expense: amounts do not match but same exchange rate is used")
    }

    @Test
    fun `should fail on expense validation if expense is finalized but amounts are missing`() {
        val expense = Prototypes.expense(
            status = ExpenseStatus.FINALIZED,
            currency = "--${workspace.defaultCurrency}--",
            convertedAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
            incomeTaxableAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency()
        )
        assertThatThrownBy { expenseService.validateExpenseConsistency(expense) }
            .hasMessage("Inconsistent expense: amounts are not provided for finalized expense")
    }

    @Test
    fun `should successfully validate valid expense`() {
        val expense = Prototypes.expense(
            status = ExpenseStatus.FINALIZED,
            currency = "--${workspace.defaultCurrency}--",
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(100),
            incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(200)
        )
        expenseService.validateExpenseConsistency(expense)
    }

    @Test
    fun `should reset the values for default currency`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(33),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(33),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = workspace.defaultCurrency,
                workspace = workspace,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(45),
            expectedIncomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(45),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = ExpenseStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )

    @Test
    fun `should calculate percent on business for default currency`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(33),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(33),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = workspace.defaultCurrency,
                workspace = workspace,
                percentOnBusiness = 10,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 45,
                adjustedAmountInDefaultCurrency = 5
            ),
            expectedIncomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 45,
                adjustedAmountInDefaultCurrency = 5
            ),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = ExpenseStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )

    @Test
    fun `should calculate tax on default currency`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 4500,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(33),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(33),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = workspace.defaultCurrency,
                workspace = workspace,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 4500,
            expectedConvertedAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4500,
                adjustedAmountInDefaultCurrency = 4091 // base for 10% added tax
            ),
            expectedIncomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4500,
                adjustedAmountInDefaultCurrency = 4091
            ),
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 409, // original amount - reported amount
            expectedGeneralTaxRateInBps = 10_00,
            expectedStatus = ExpenseStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )

    @Test
    fun `should calculate tax on default currency with percent on business`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 450,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(33),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(33),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = workspace.defaultCurrency,
                workspace = workspace,
                percentOnBusiness = 90,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 450,
            expectedConvertedAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 450,
                adjustedAmountInDefaultCurrency = 368 // 90% of 450 = 405, base for 10% added tax is 368
            ),
            expectedIncomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 450,
                adjustedAmountInDefaultCurrency = 368
            ),
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 37, // business spent amount 405 - base amount 368
            expectedGeneralTaxRateInBps = 10_00,
            expectedStatus = ExpenseStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )

    @Test
    fun `should keep amounts as null if not yet converted`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
                incomeTaxableAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
            expectedIncomeTaxableAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = ExpenseStatus.PENDING_CONVERSION,
            expectedUseDifferentExchangeRates = true
        )

    @Test
    fun `should set amount to null if not yet converted and same rate used for conversion`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = LegacyAmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 42
                ),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(44),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 100,
                status = ExpenseStatus.FINALIZED,
                useDifferentExchangeRateForIncomeTaxPurposes = false
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
            expectedIncomeTaxableAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = ExpenseStatus.PENDING_CONVERSION
        )

    @Test
    fun `should keep income taxable amount if not yet converted and different rate used for conversion`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = LegacyAmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 42
                ),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(44),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 100,
                status = ExpenseStatus.FINALIZED,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
            expectedIncomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(44),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.PENDING_CONVERSION
        )

    @Test
    fun `should propagate converted amount if same rate is used`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = ExpenseStatus.FINALIZED
        )

    @Test
    fun `should keep income taxable amount if different rate is used`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(100),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.FINALIZED
        )

    @Test
    fun `should keep income taxable amount empty if different rate is used`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
                incomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 300
                ),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = Prototypes.legacyEmptyAmountsInDefaultCurrency(),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
        )

    @Test
    fun `should use converted amounts to calculate percent on business`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(41),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                percentOnBusiness = 90,
                status = ExpenseStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 30,
                adjustedAmountInDefaultCurrency = 27 // 90% of amount of 30
            ),
            expectedIncomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 41,
                adjustedAmountInDefaultCurrency = 37 // 90% of actual amount 41
            ),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.FINALIZED
        )

    @Test
    fun `should calculate tax based on income taxable amount if different rate is used`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(30),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(41),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 30,
                adjustedAmountInDefaultCurrency = 27 // base for 10% added tax to get 30
            ),
            expectedIncomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 41,
                adjustedAmountInDefaultCurrency = 37 // base for 10% added tax to get 41
            ),
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 4, // actual amount 41 - base amount 37
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.FINALIZED
        )

    @Test
    fun `should calculate tax bases on converted amount if same rate is used`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 45,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(41),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 100,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 41,
                adjustedAmountInDefaultCurrency = 37 // base for 10% added tax to get 41
            ),
            expectedIncomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 41,
                adjustedAmountInDefaultCurrency = 37
            ),
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 4, // actual amount 41 - base amount 37
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = ExpenseStatus.FINALIZED
        )

    @Test
    fun `should calculate tax based on income taxable amount if percent on business provided and different rate used`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 4500,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(4000),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(4100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 80,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 4500,
            expectedConvertedAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4000,
                adjustedAmountInDefaultCurrency = 2909  // 80% of 4000 = 3200, base for 10% added tax for 3200 is 2909
            ),
            expectedIncomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4100,
                adjustedAmountInDefaultCurrency = 2982 // 80% of 4100 = 3280, base for 10% added tax for 3280 is 2982
            ),
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 298, // 3280 of taxable amount - base amount of 2982
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = ExpenseStatus.FINALIZED
        )

    @Test
    fun `should calculate tax based on converted amount if percent on business provided and same rate used`() =
        executeSaveExpenseAndAssert(
            expense = Prototypes.expense(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 4500,
                convertedAmounts = Prototypes.legacyAmountsInDefaultCurrency(4000),
                incomeTaxableAmounts = Prototypes.legacyAmountsInDefaultCurrency(4100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                percentOnBusiness = 80,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 4500,
            expectedConvertedAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4000,
                adjustedAmountInDefaultCurrency = 2909  // 80% of 4000 = 3200, base for 10% added tax for 3200 is 2909
            ),
            expectedIncomeTaxableAmounts = LegacyAmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 4000,
                adjustedAmountInDefaultCurrency = 2909
            ),
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 291, // based on converted amount of 3200
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = ExpenseStatus.FINALIZED
        )

    private fun executeSaveExpenseAndAssert(
        expense: Expense,
        expectedOriginalAmount: Long,
        expectedConvertedAmounts: LegacyAmountsInDefaultCurrency,
        expectedIncomeTaxableAmounts: LegacyAmountsInDefaultCurrency,
        expectedGeneralTax: GeneralTax?,
        expectedGeneralTaxRateInBps: Int?,
        expectedGeneralTaxAmount: Long?,
        expectedStatus: ExpenseStatus,
        expectedUseDifferentExchangeRates: Boolean
    ) {
        setupSaveExpenseMock()

        val actualExpense = runBlocking {
            expenseService.saveExpense(expense)
        }

        assertThat(actualExpense.originalAmount).isEqualTo(expectedOriginalAmount)
        assertThat(actualExpense.status).isEqualTo(expectedStatus)
        assertThat(actualExpense.convertedAmounts).isEqualTo(expectedConvertedAmounts)
        assertThat(actualExpense.incomeTaxableAmounts).isEqualTo(expectedIncomeTaxableAmounts)
        assertThat(actualExpense.generalTax).isEqualTo(expectedGeneralTax)
        assertThat(actualExpense.generalTaxAmount).isEqualTo(expectedGeneralTaxAmount)
        assertThat(actualExpense.generalTaxRateInBps).isEqualTo(expectedGeneralTaxRateInBps)
        assertThat(actualExpense.useDifferentExchangeRateForIncomeTaxPurposes)
            .isEqualTo(expectedUseDifferentExchangeRates)
    }

    private fun setupSaveExpenseMock() {
        whenever(expenseRepository.save(any<Expense>())) doAnswer { invocation ->
            invocation.arguments[0] as Expense
        }
    }
}
