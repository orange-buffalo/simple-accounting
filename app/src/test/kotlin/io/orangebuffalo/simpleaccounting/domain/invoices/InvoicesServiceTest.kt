package io.orangebuffalo.simpleaccounting.domain.invoices

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.database.EntitiesFactoryInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentDate
import io.orangebuffalo.simpleaccounting.infra.TimeService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
import java.util.stream.Stream

@SimpleAccountingIntegrationTest
class InvoicesServiceTest(
    @Autowired private val invoicesService: InvoicesService,
    @Autowired private val invoiceRepository: InvoiceRepository,
    @Autowired private val entitiesFactoryInfra: EntitiesFactoryInfra,
    private val preconditionsFactory: PreconditionsFactory,
) {

    @field:MockBean
    private lateinit var timeService: TimeService

    @Test
    fun `should move proper invoices into Overdue`() {
        val testData = setupOverdueTestPreconditions()
        mockCurrentDate(timeService)

        invoicesService.moveInvoicesToOverdue()

        testData.overdueInvoices.forEach {
            val maybeInvoice = invoiceRepository.findById(it.id!!)
            assertThat(maybeInvoice).hasValueSatisfying { invoice ->
                assertThat(invoice.status).isEqualTo(InvoiceStatus.OVERDUE)
            }
        }

        testData.unchangedInvoices.forEach {
            val maybeInvoice = invoiceRepository.findById(it.id!!)
            assertThat(maybeInvoice).hasValueSatisfying { invoice ->
                assertThat(invoice.status).isNotEqualTo(InvoiceStatus.OVERDUE)
            }
        }
    }

    private fun setupOverdueTestPreconditions() = preconditionsFactory.setup {
        object {
            val firstOwner = platformUser()
            val secondOwner = platformUser()
            val firstWorkspace = workspace(owner = firstOwner)
            val secondWorkspace = workspace(owner = secondOwner)
            val firstCustomer = customer(workspace = firstWorkspace)
            val secondCustomer = customer(workspace = secondWorkspace)
            val overdueInvoices = listOf(
                invoice(
                    customer = firstCustomer,
                    status = InvoiceStatus.SENT,
                    dueDate = MOCK_DATE.minusDays(1)
                ),
                invoice(
                    customer = secondCustomer,
                    status = InvoiceStatus.SENT,
                    dueDate = MOCK_DATE.minusDays(1)
                )
            )
            val unchangedInvoices = listOf(
                invoice(
                    customer = secondCustomer,
                    status = InvoiceStatus.SENT,
                    dueDate = MOCK_DATE
                ),
                invoice(
                    customer = secondCustomer,
                    status = InvoiceStatus.SENT,
                    dueDate = MOCK_DATE.plusDays(1)
                ),
                invoice(
                    customer = firstCustomer,
                    status = InvoiceStatus.DRAFT,
                    dueDate = MOCK_DATE.minusDays(1)
                ),
                invoice(
                    customer = secondCustomer,
                    status = InvoiceStatus.DRAFT,
                    dueDate = MOCK_DATE
                ),
                invoice(
                    customer = secondCustomer,
                    status = InvoiceStatus.PAID,
                    dueDate = MOCK_DATE.minusDays(1)
                ),
                invoice(
                    customer = firstCustomer,
                    status = InvoiceStatus.PAID,
                    dueDate = MOCK_DATE
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("invoiceStatusTestData")
    @WithMockFryUser
    fun `should save invoice and set proper status`(testDataFactory: (EntitiesFactoryInfra) -> InvoiceStatusTestData) {
        val testData = testDataFactory(entitiesFactoryInfra)
        mockCurrentDate(timeService)

        val savedInvoice = runBlocking {
            invoicesService.saveInvoice(
                invoice = testData.invoice,
                workspaceId = testData.workspace.id!!
            )
        }

        assertThat(savedInvoice.id).isEqualTo(testData.invoice.id)
        assertThat(savedInvoice.status).isEqualTo(testData.expectedStatus)
    }

    companion object TestDataHolder {
        @Suppress("unused")
        @JvmStatic
        fun invoiceStatusTestData(): Stream<(EntitiesFactoryInfra) -> InvoiceStatusTestData> = Stream.of(
            { preconditionsInfra ->
                InvoiceStatusTestData(
                    entitiesFactory = EntitiesFactory(preconditionsInfra),
                    expectedStatus = InvoiceStatus.DRAFT,
                )
            },

            { preconditionsInfra ->
                InvoiceStatusTestData(
                    entitiesFactory = EntitiesFactory(preconditionsInfra),
                    currentStatus = InvoiceStatus.DRAFT,
                    dueDate = MOCK_DATE.minusDays(1),
                    expectedStatus = InvoiceStatus.DRAFT
                )
            },

            { preconditionsInfra ->
                InvoiceStatusTestData(
                    entitiesFactory = EntitiesFactory(preconditionsInfra),
                    dateSent = MOCK_DATE,
                    expectedStatus = InvoiceStatus.SENT,
                )
            },

            { preconditionsInfra ->
                InvoiceStatusTestData(
                    entitiesFactory = EntitiesFactory(preconditionsInfra),
                    datePaid = MOCK_DATE,
                    currentStatus = InvoiceStatus.DRAFT,
                    expectedStatus = InvoiceStatus.PAID,
                )
            },

            { preconditionsInfra ->
                InvoiceStatusTestData(
                    entitiesFactory = EntitiesFactory(preconditionsInfra),
                    datePaid = MOCK_DATE,
                    dueDate = MOCK_DATE.minusDays(1),
                    currentStatus = InvoiceStatus.DRAFT,
                    expectedStatus = InvoiceStatus.PAID,
                )
            },

            { preconditionsInfra ->
                InvoiceStatusTestData(
                    entitiesFactory = EntitiesFactory(preconditionsInfra),
                    dateSent = MOCK_DATE,
                    dueDate = MOCK_DATE.minusDays(1),
                    expectedStatus = InvoiceStatus.OVERDUE,
                )
            },

            { preconditionsInfra ->
                InvoiceStatusTestData(
                    entitiesFactory = EntitiesFactory(preconditionsInfra),
                    dateSent = MOCK_DATE,
                    dueDate = MOCK_DATE,
                    expectedStatus = InvoiceStatus.SENT,
                )
            },

            { preconditionsInfra ->
                InvoiceStatusTestData(
                    entitiesFactory = EntitiesFactory(preconditionsInfra),
                    dateSent = MOCK_DATE,
                    dueDate = MOCK_DATE.plusDays(1),
                    expectedStatus = InvoiceStatus.SENT,
                )
            }
        )
    }

    class InvoiceStatusTestData(
        entitiesFactory: EntitiesFactory,
        val dateSent: LocalDate? = null,
        val datePaid: LocalDate? = null,
        val dueDate: LocalDate = MOCK_DATE,
        val currentStatus: InvoiceStatus = InvoiceStatus.DRAFT,
        val expectedStatus: InvoiceStatus,
    ) {
        val fry = entitiesFactory.fry()
        val workspace = entitiesFactory.workspace(owner = fry)
        val invoice = entitiesFactory.invoice(
            dateSent = dateSent,
            datePaid = datePaid,
            dueDate = dueDate,
            status = currentStatus,
            customer = entitiesFactory.customer(workspace = workspace),
        )
    }
}
