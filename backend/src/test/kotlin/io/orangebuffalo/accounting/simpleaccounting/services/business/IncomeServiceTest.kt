package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.AmountsInDefaultCurrency
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.GeneralTax
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.IncomeStatus
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeRepository
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
@DisplayName("IncomeService")
internal class IncomeServiceTest {

    @field:Mock
    private lateinit var incomeRepository: IncomeRepository

    @field:Mock
    private lateinit var entityManagerFactory: EntityManagerFactory

    @field:InjectMocks
    private lateinit var incomeService: IncomeService

    private val workspace = Prototypes.workspace()
    private val generalTaxFromWorkspace = Prototypes.generalTax(workspace = workspace, rateInBps = 10_00)

    @Test
    fun `should fail on income validation if income is in default currency and converted amount differs`() {
        val income = Prototypes.income(
            currency = workspace.defaultCurrency,
            originalAmount = 200,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(199)
        )
        assertThatThrownBy { incomeService.validateIncomeConsistency(income) }
            .hasMessage("Inconsistent income: converted amount does not match original for default currency")
    }

    @Test
    fun `should fail on income validation if income is in default currency and income taxable amount differs`() {
        val income = Prototypes.income(
            currency = workspace.defaultCurrency,
            originalAmount = 200,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(200),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(199)
        )
        assertThatThrownBy { incomeService.validateIncomeConsistency(income) }
            .hasMessage("Inconsistent income: income taxable amount does not match original for default currency")
    }

    @Test
    fun `should fail on income validation if same conversion rate is used but amounts differ`() {
        val income = Prototypes.income(
            currency = "--${workspace.defaultCurrency}--",
            originalAmount = 200,
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(200),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(199)
        )
        assertThatThrownBy { incomeService.validateIncomeConsistency(income) }
            .hasMessage("Inconsistent income: amounts do not match but same exchange rate is used")
    }

    @Test
    fun `should fail on income validation if income is finalized but amounts are missing`() {
        val income = Prototypes.income(
            status = IncomeStatus.FINALIZED,
            currency = "--${workspace.defaultCurrency}--",
            convertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency()
        )
        assertThatThrownBy { incomeService.validateIncomeConsistency(income) }
            .hasMessage("Inconsistent income: amounts are not provided for finalized income")
    }

    @Test
    fun `should successfully validate valid income`() {
        val income = Prototypes.income(
            status = IncomeStatus.FINALIZED,
            currency = "--${workspace.defaultCurrency}--",
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            convertedAmounts = Prototypes.amountsInDefaultCurrency(100),
            incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(200)
        )
        incomeService.validateIncomeConsistency(income)
    }

    @Test
    fun `should reset the values for default currency`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(33),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(33),
                status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = workspace.defaultCurrency,
                workspace = workspace,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.amountsInDefaultCurrency(45),
            expectedIncomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(45),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = IncomeStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )

    @Test
    fun `should calculate tax on default currency`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 4500,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(33),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(33),
                status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = workspace.defaultCurrency,
                workspace = workspace,
                useDifferentExchangeRateForIncomeTaxPurposes = true
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
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 409, // original amount - reported amount
            expectedGeneralTaxRateInBps = 10_00,
            expectedStatus = IncomeStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )

    @Test
    fun `should keep amounts as null if not yet converted`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            expectedIncomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = IncomeStatus.PENDING_CONVERSION,
            expectedUseDifferentExchangeRates = true
        )

    @Test
    fun `should set amount to null if not yet converted and same rate used for conversion`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 42
                ),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(44),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                status = IncomeStatus.FINALIZED,
                useDifferentExchangeRateForIncomeTaxPurposes = false
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            expectedIncomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = IncomeStatus.PENDING_CONVERSION
        )

    @Test
    fun `should keep income taxable amount if not yet converted and different rate used for conversion`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 42
                ),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(44),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                status = IncomeStatus.FINALIZED,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            expectedIncomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(44),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = IncomeStatus.PENDING_CONVERSION
        )

    @Test
    fun `should propagate converted amount if same rate is used`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(30),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = IncomeStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.amountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(30),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = IncomeStatus.FINALIZED
        )

    @Test
    fun `should keep income taxable amount if different rate is used`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(30),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = IncomeStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.amountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(100),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = IncomeStatus.FINALIZED
        )

    @Test
    fun `should keep income taxable amount empty if different rate is used`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = null,
                originalAmount = 45,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 300
                ),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = IncomeStatus.PENDING_CONVERSION
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = Prototypes.amountsInDefaultCurrency(30),
            expectedIncomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
        )

    @Test
    fun `should calculate tax based on income taxable amount if different rate is used`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 45,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(30),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(41),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = IncomeStatus.PENDING_CONVERSION
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
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 4, // actual amount 41 - base amount 37
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = IncomeStatus.FINALIZED
        )

    @Test
    fun `should calculate tax bases on converted amount if same rate is used`() =
        executeSaveIncomeAndAssert(
            income = Prototypes.income(
                generalTax = generalTaxFromWorkspace,
                originalAmount = 45,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(41),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                workspace = workspace,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = IncomeStatus.PENDING_CONVERSION
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
            expectedGeneralTax = generalTaxFromWorkspace,
            expectedGeneralTaxAmount = 4, // actual amount 41 - base amount 37
            expectedGeneralTaxRateInBps = 10_00,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = IncomeStatus.FINALIZED
        )

    private fun executeSaveIncomeAndAssert(
        income: Income,
        expectedOriginalAmount: Long,
        expectedConvertedAmounts: AmountsInDefaultCurrency,
        expectedIncomeTaxableAmounts: AmountsInDefaultCurrency,
        expectedGeneralTax: GeneralTax?,
        expectedGeneralTaxRateInBps: Int?,
        expectedGeneralTaxAmount: Long?,
        expectedStatus: IncomeStatus,
        expectedUseDifferentExchangeRates: Boolean
    ) {
        setupSaveIncomeMock()

        val actualIncome = runBlocking {
            incomeService.saveIncome(income)
        }

        assertThat(actualIncome.originalAmount).isEqualTo(expectedOriginalAmount)
        assertThat(actualIncome.status).isEqualTo(expectedStatus)
        assertThat(actualIncome.convertedAmounts).isEqualTo(expectedConvertedAmounts)
        assertThat(actualIncome.incomeTaxableAmounts).isEqualTo(expectedIncomeTaxableAmounts)
        assertThat(actualIncome.generalTax).isEqualTo(expectedGeneralTax)
        assertThat(actualIncome.generalTaxAmount).isEqualTo(expectedGeneralTaxAmount)
        assertThat(actualIncome.generalTaxRateInBps).isEqualTo(expectedGeneralTaxRateInBps)
        assertThat(actualIncome.useDifferentExchangeRateForIncomeTaxPurposes)
            .isEqualTo(expectedUseDifferentExchangeRates)
    }

    private fun setupSaveIncomeMock() {
        whenever(incomeRepository.save(any<Income>())) doAnswer { invocation ->
            invocation.arguments[0] as Income
        }
    }
}
