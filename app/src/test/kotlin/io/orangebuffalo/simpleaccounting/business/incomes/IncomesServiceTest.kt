package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate

@DisplayName("IncomeService")
internal class IncomesServiceTest(
    @Autowired private val incomesService: IncomesService,
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
            val invoiceFromWorkspace = invoice(
                customer = customer(workspace = workspace),
            )
        }
    }

    @Test
    @WithMockFryUser
    fun `should reset the values for default currency`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(33, 33),
                incomeTaxableAmounts = AmountsInDefaultCurrency(33, 33),
                status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.workspace.defaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(45, 45),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(45, 45),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = IncomeStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax on default currency`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 4500,
                convertedAmounts = AmountsInDefaultCurrency(33, 33),
                incomeTaxableAmounts = AmountsInDefaultCurrency(33, 33),
                status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.workspace.defaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
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
            expectedStatus = IncomeStatus.FINALIZED,
            expectedUseDifferentExchangeRates = false
        )
    }

    @Test
    @WithMockFryUser
    fun `should keep amounts as null if not yet converted`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(null, null),
                incomeTaxableAmounts = AmountsInDefaultCurrency(null, null),
                status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                workspaceId = preconditions.workspace.id!!,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(null, null),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(null, null),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedStatus = IncomeStatus.PENDING_CONVERSION,
            expectedUseDifferentExchangeRates = true
        )
    }

    @Test
    @WithMockFryUser
    fun `should set amount to null if not yet converted and same rate used for conversion`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 42
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(44, 44),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                status = IncomeStatus.FINALIZED,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(null, null),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(null, null),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = IncomeStatus.PENDING_CONVERSION
        )
    }

    @Test
    @WithMockFryUser
    fun `should keep income taxable amount if not yet converted and different rate used for conversion`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 42
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(44, 44),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                status = IncomeStatus.FINALIZED,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(null, null),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(44, 44),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = IncomeStatus.PENDING_CONVERSION
        )
    }

    @Test
    @WithMockFryUser
    fun `should propagate converted amount if same rate is used`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30, 30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(100, 100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = IncomeStatus.PENDING_CONVERSION,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(30, 30),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(30, 30),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = false,
            expectedStatus = IncomeStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should keep income taxable amount if different rate is used`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30, 30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(100, 100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = IncomeStatus.PENDING_CONVERSION,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(30, 30),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(100, 100),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = IncomeStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should keep income taxable amount empty if different rate is used`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = null,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30, 30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = null,
                    adjustedAmountInDefaultCurrency = 300
                ),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = IncomeStatus.PENDING_CONVERSION,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
            ),
            expectedOriginalAmount = 45,
            expectedConvertedAmounts = AmountsInDefaultCurrency(30, 30),
            expectedIncomeTaxableAmounts = AmountsInDefaultCurrency(null, null),
            expectedGeneralTax = null,
            expectedGeneralTaxAmount = null,
            expectedGeneralTaxRateInBps = null,
            expectedUseDifferentExchangeRates = true,
            expectedStatus = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax based on income taxable amount if different rate is used`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(30, 30),
                incomeTaxableAmounts = AmountsInDefaultCurrency(41, 41),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = IncomeStatus.PENDING_CONVERSION,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
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
            expectedStatus = IncomeStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax bases on converted amount if same rate is used`() {
        executeSaveIncomeAndAssert(
            income = Income(
                generalTaxId = preconditions.generalTaxFromWorkspace.id,
                originalAmount = 45,
                convertedAmounts = AmountsInDefaultCurrency(41, 41),
                incomeTaxableAmounts = AmountsInDefaultCurrency(100, 100),
                generalTaxRateInBps = 33,
                generalTaxAmount = 33,
                currency = preconditions.nonDefaultCurrency,
                workspaceId = preconditions.workspace.id!!,
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = IncomeStatus.PENDING_CONVERSION,
                categoryId = null,
                dateReceived = LocalDate.now(),
                title = "test",
                timeRecorded = Instant.now(),
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
            expectedStatus = IncomeStatus.FINALIZED
        )
    }

    @Test
    @WithMockFryUser
    fun `should validate invoice if provided`() {
        assertThatThrownBy {
            runBlocking {
                incomesService.saveIncome(
                    Income(
                        generalTaxId = preconditions.generalTaxFromWorkspace.id,
                        originalAmount = 45,
                        convertedAmounts = AmountsInDefaultCurrency(41, 41),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(100, 100),
                        generalTaxRateInBps = 33,
                        generalTaxAmount = 33,
                        currency = preconditions.nonDefaultCurrency,
                        workspaceId = preconditions.workspace.id!!,
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        status = IncomeStatus.PENDING_CONVERSION,
                        categoryId = null,
                        dateReceived = LocalDate.now(),
                        title = "test",
                        timeRecorded = Instant.now(),
                        linkedInvoiceId = 100
                    ),
                )
            }
        }.isInstanceOf(EntityNotFoundException::class.java).hasMessage("Invoice 100 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should update invoice if provided`() {
        val invoiceId = preconditions.invoiceFromWorkspace.id

        runBlocking {
            incomesService.saveIncome(
                Income(
                    generalTaxId = preconditions.generalTaxFromWorkspace.id,
                    originalAmount = 45,
                    convertedAmounts = AmountsInDefaultCurrency(41, 41),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(100, 100),
                    generalTaxRateInBps = 33,
                    generalTaxAmount = 33,
                    currency = preconditions.nonDefaultCurrency,
                    workspaceId = preconditions.workspace.id!!,
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = IncomeStatus.PENDING_CONVERSION,
                    categoryId = null,
                    title = "test",
                    timeRecorded = Instant.now(),
                    linkedInvoiceId = invoiceId,
                    dateReceived = LocalDate.of(3000, 5, 13),
                ),
            )
        }

        assertThat(aggregateTemplate.findAll(Invoice::class.java))
            .filteredOn { it.id == invoiceId }
            .extracting(Invoice::datePaid)
            .containsExactly(tuple(LocalDate.of(3000, 5, 13)))
    }

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
        val actualIncome = runBlocking {
            incomesService.saveIncome(income)
        }

        assertThat(actualIncome.originalAmount).isEqualTo(expectedOriginalAmount)
        assertThat(actualIncome.status).isEqualTo(expectedStatus)
        assertThat(actualIncome.convertedAmounts).isEqualTo(expectedConvertedAmounts)
        assertThat(actualIncome.incomeTaxableAmounts).isEqualTo(expectedIncomeTaxableAmounts)
        assertThat(actualIncome.generalTaxId).isEqualTo(expectedGeneralTax?.id)
        assertThat(actualIncome.generalTaxAmount).isEqualTo(expectedGeneralTaxAmount)
        assertThat(actualIncome.generalTaxRateInBps).isEqualTo(expectedGeneralTaxRateInBps)
        assertThat(actualIncome.useDifferentExchangeRateForIncomeTaxPurposes)
            .isEqualTo(expectedUseDifferentExchangeRates)
    }
}
