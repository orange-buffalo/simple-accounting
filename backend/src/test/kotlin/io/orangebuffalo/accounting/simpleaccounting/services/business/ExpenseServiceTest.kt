package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Tax
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class ExpenseServiceTest {

    @field:Mock
    private lateinit var expenseRepository: ExpenseRepository

    @field:InjectMocks
    private lateinit var expenseService: ExpenseService

    private val workspace = Prototypes.workspace()
    private val tax = Prototypes.tax(workspace = workspace, rateInBps = 10_00)

    @BeforeEach
    fun setup() {
        whenever(expenseRepository.save(any<Expense>())) doAnswer { invocation ->
            invocation.arguments[0] as Expense
        }
    }

    @Test
    fun `should set reset the values for default currency`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = null,
                originalAmount = 45,
                amountInDefaultCurrency = 33,
                actualAmountInDefaultCurrency = 33,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = workspace.defaultCurrency,
                percentOnBusiness = 100
            ),
            expectedOriginalAmount = 45,
            expectedAmountInDefaultCurrency = 45,
            expectedActualAmountInDefaultCurrency = 45,
            expectedReportedAmountInDefaultCurrency = 45,
            expectedTax = null,
            expectedTaxAmount = null,
            expectedTaxRateInBps = null
        )

    @Test
    fun `should calculate percent on business for default currency`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = null,
                originalAmount = 45,
                amountInDefaultCurrency = 33,
                actualAmountInDefaultCurrency = 33,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = workspace.defaultCurrency,
                percentOnBusiness = 10
            ),
            expectedOriginalAmount = 45,
            expectedAmountInDefaultCurrency = 45,
            expectedActualAmountInDefaultCurrency = 45,
            expectedReportedAmountInDefaultCurrency = 5,
            expectedTax = null,
            expectedTaxAmount = null,
            expectedTaxRateInBps = null
        )

    @Test
    fun `should calculate tax on default currency`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = tax,
                originalAmount = 4500,
                amountInDefaultCurrency = 33,
                actualAmountInDefaultCurrency = 33,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = workspace.defaultCurrency,
                percentOnBusiness = 100
            ),
            expectedOriginalAmount = 4500,
            expectedAmountInDefaultCurrency = 4500,
            expectedActualAmountInDefaultCurrency = 4500,
            expectedReportedAmountInDefaultCurrency = 4091, // base for 10% added tax
            expectedTax = tax,
            expectedTaxAmount = 409, // original amount - reported amount
            expectedTaxRateInBps = 10_00
        )

    @Test
    fun `should calculate tax on default currency with percent on business`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = tax,
                originalAmount = 450,
                amountInDefaultCurrency = 33,
                actualAmountInDefaultCurrency = 33,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = workspace.defaultCurrency,
                percentOnBusiness = 90
            ),
            expectedOriginalAmount = 450,
            expectedAmountInDefaultCurrency = 450,
            expectedActualAmountInDefaultCurrency = 450,
            expectedReportedAmountInDefaultCurrency = 368, // 90% of 450 = 405, base for 10% added tax is 368
            expectedTax = tax,
            expectedTaxAmount = 37, // business spent amount 405 - base amount 368
            expectedTaxRateInBps = 10_00
        )

    @Test
    fun `should keep amounts at 0 if not yet converted`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = null,
                originalAmount = 45,
                amountInDefaultCurrency = 0,
                actualAmountInDefaultCurrency = 33,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                percentOnBusiness = 100
            ),
            expectedOriginalAmount = 45,
            expectedAmountInDefaultCurrency = 0,
            expectedActualAmountInDefaultCurrency = 0,
            expectedReportedAmountInDefaultCurrency = 0,
            expectedTax = null,
            expectedTaxAmount = null,
            expectedTaxRateInBps = null
        )

    @Test
    fun `should keep amounts at 0 if actual amount not yet provided`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = null,
                originalAmount = 45,
                amountInDefaultCurrency = 40,
                actualAmountInDefaultCurrency = 0,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                percentOnBusiness = 100
            ),
            expectedOriginalAmount = 45,
            expectedAmountInDefaultCurrency = 40,
            expectedActualAmountInDefaultCurrency = 0,
            expectedReportedAmountInDefaultCurrency = 0,
            expectedTax = null,
            expectedTaxAmount = null,
            expectedTaxRateInBps = null
        )

    @Test
    fun `should use actual amount if provided`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = null,
                originalAmount = 45,
                amountInDefaultCurrency = 40,
                actualAmountInDefaultCurrency = 41,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                percentOnBusiness = 100
            ),
            expectedOriginalAmount = 45,
            expectedAmountInDefaultCurrency = 40,
            expectedActualAmountInDefaultCurrency = 41,
            expectedReportedAmountInDefaultCurrency = 41,
            expectedTax = null,
            expectedTaxAmount = null,
            expectedTaxRateInBps = null
        )

    @Test
    fun `should use actual amount to calculate percent on business`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = null,
                originalAmount = 45,
                amountInDefaultCurrency = 40,
                actualAmountInDefaultCurrency = 41,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                percentOnBusiness = 90
            ),
            expectedOriginalAmount = 45,
            expectedAmountInDefaultCurrency = 40,
            expectedActualAmountInDefaultCurrency = 41,
            expectedReportedAmountInDefaultCurrency = 37, // 90% of actual amount 41
            expectedTax = null,
            expectedTaxAmount = null,
            expectedTaxRateInBps = null
        )

    @Test
    fun `should calculate tax bases on actual amount`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = tax,
                originalAmount = 45,
                amountInDefaultCurrency = 40,
                actualAmountInDefaultCurrency = 41,
                reportedAmountInDefaultCurrency = 33,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                percentOnBusiness = 100
            ),
            expectedOriginalAmount = 45,
            expectedAmountInDefaultCurrency = 40,
            expectedActualAmountInDefaultCurrency = 41,
            expectedReportedAmountInDefaultCurrency = 37, // base for 10% added tax to get 41
            expectedTax = tax,
            expectedTaxAmount = 4, // actual amount 41 - base amount 37
            expectedTaxRateInBps = 10_00
        )

    @Test
    fun `should calculate tax bases on actual amount if percent on business provided`() =
        executeSaveExpenseAndAssert(
            expense = createExpense(
                tax = tax,
                originalAmount = 4500,
                amountInDefaultCurrency = 4000,
                actualAmountInDefaultCurrency = 4100,
                reportedAmountInDefaultCurrency = 3300,
                taxRateInBps = 33,
                taxAmount = 33,
                currency = "--${workspace.defaultCurrency}--",
                percentOnBusiness = 80
            ),
            expectedOriginalAmount = 4500,
            expectedAmountInDefaultCurrency = 4000,
            expectedActualAmountInDefaultCurrency = 4100,
            expectedReportedAmountInDefaultCurrency = 2982, // 80% of 4100 = 3280, base for 10% added tax for 3280 is 2982
            expectedTax = tax,
            expectedTaxAmount = 298, // 3280 of actual amount - base amount of 2982
            expectedTaxRateInBps = 10_00
        )

    private fun executeSaveExpenseAndAssert(
        expense: Expense,
        expectedOriginalAmount: Long,
        expectedAmountInDefaultCurrency: Long,
        expectedActualAmountInDefaultCurrency: Long,
        expectedTax: Tax?,
        expectedTaxRateInBps: Int? = null,
        expectedTaxAmount: Long? = null,
        expectedReportedAmountInDefaultCurrency: Long
    ) {
        val actualExpense = runBlocking {
            expenseService.saveExpense(expense)
        }

        assertThat(actualExpense.originalAmount).isEqualTo(expectedOriginalAmount)
        assertThat(actualExpense.amountInDefaultCurrency).isEqualTo(expectedAmountInDefaultCurrency)
        assertThat(actualExpense.actualAmountInDefaultCurrency).isEqualTo(expectedActualAmountInDefaultCurrency)
        assertThat(actualExpense.reportedAmountInDefaultCurrency).isEqualTo(expectedReportedAmountInDefaultCurrency)
        assertThat(actualExpense.tax).isEqualTo(expectedTax)
        assertThat(actualExpense.taxAmount).isEqualTo(expectedTaxAmount)
        assertThat(actualExpense.taxRateInBps).isEqualTo(expectedTaxRateInBps)
    }

    private fun createExpense(
        currency: String,
        originalAmount: Long,
        amountInDefaultCurrency: Long,
        actualAmountInDefaultCurrency: Long,
        percentOnBusiness: Int,
        tax: Tax?,
        taxRateInBps: Int? = null,
        taxAmount: Long? = null,
        reportedAmountInDefaultCurrency: Long
    ) = Expense(
        currency = currency,
        tax = tax,
        originalAmount = originalAmount,
        amountInDefaultCurrency = amountInDefaultCurrency,
        actualAmountInDefaultCurrency = actualAmountInDefaultCurrency,
        reportedAmountInDefaultCurrency = reportedAmountInDefaultCurrency,
        percentOnBusiness = percentOnBusiness,
        taxRateInBps = taxRateInBps,
        taxAmount = taxAmount,
        category = Prototypes.category(workspace = workspace),
        workspace = workspace,
        title = "title",
        timeRecorded = Instant.MIN,
        datePaid = LocalDate.MIN
    )
}