package io.orangebuffalo.simpleaccounting.domain.invoices

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.stub
import io.orangebuffalo.simpleaccounting.MOCK_DATE
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.mockCurrentDate
import io.orangebuffalo.simpleaccounting.services.business.CustomerService
import io.orangebuffalo.simpleaccounting.services.business.GeneralTaxService
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.core.task.AsyncTaskExecutor
import java.time.LocalDate
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoicesServiceTest {

    @field:Mock
    lateinit var invoiceRepository: InvoiceRepository

    @field:Mock
    lateinit var customerService: CustomerService

    @field:Mock
    lateinit var generalTaxService: GeneralTaxService

    @field:Mock
    lateinit var workspaceService: WorkspaceService

    @field:Mock
    lateinit var documentsService: DocumentsService

    @field:Mock
    lateinit var timeService: TimeService

    @field:Mock
    lateinit var taskExecutor: AsyncTaskExecutor

    @field:InjectMocks
    lateinit var invoicesService: InvoicesService

    @ParameterizedTest
    @MethodSource("invoiceStatusTestData")
    fun `should save invoice and set proper status`(invoiceToSave: Invoice, expectedInvoiceStatus: InvoiceStatus) {
        invoiceRepository.stub {
            onBlocking { save(any<Invoice>()) } doAnswer { invocationOnMock -> invocationOnMock.getArgument(0) }
        }
        mockCurrentDate(timeService)

        val savedInvoice = runBlocking { invoicesService.saveInvoice(invoiceToSave, 42) }

        assertThat(savedInvoice.id).isEqualTo(invoiceToSave.id)
        assertThat(savedInvoice.status).isEqualTo(expectedInvoiceStatus)
    }

    companion object TestDataHolder {
        @Suppress("unused")
        @JvmStatic
        fun invoiceStatusTestData() : Stream<Arguments> = Stream.of(
            Arguments.of(
                invoice(),
                InvoiceStatus.DRAFT
            ),

            Arguments.of(
                Prototypes.invoice(
                    status = InvoiceStatus.DRAFT,
                    dueDate = MOCK_DATE.minusDays(1)
                ),
                InvoiceStatus.DRAFT
            ),

            Arguments.of(
                invoice(
                    dateSent = MOCK_DATE
                ),
                InvoiceStatus.SENT
            ),

            Arguments.of(
                Prototypes.invoice(
                    datePaid = MOCK_DATE,
                    status = InvoiceStatus.DRAFT
                ),
                InvoiceStatus.PAID
            ),

            Arguments.of(
                Prototypes.invoice(
                    datePaid = MOCK_DATE,
                    dueDate = MOCK_DATE.minusDays(1),
                    status = InvoiceStatus.DRAFT
                ),
                InvoiceStatus.PAID
            ),

            Arguments.of(
                Prototypes.invoice(
                    dateSent = MOCK_DATE,
                    dueDate = MOCK_DATE.minusDays(1)
                ),
                InvoiceStatus.OVERDUE
            ),

            Arguments.of(
                Prototypes.invoice(
                    dateSent = MOCK_DATE,
                    dueDate = MOCK_DATE
                ),
                InvoiceStatus.SENT
            ),

            Arguments.of(
                Prototypes.invoice(
                    dateSent = MOCK_DATE,
                    dueDate = MOCK_DATE.plusDays(1)
                ),
                InvoiceStatus.SENT
            )
        )

        private fun invoice(
            dateSent: LocalDate? = null,
            datePaid: LocalDate? = null,
            dueDate: LocalDate = MOCK_DATE,
            status: InvoiceStatus = InvoiceStatus.DRAFT
        ) = Prototypes.invoice(
            datePaid = datePaid,
            dueDate = dueDate,
            dateSent = dateSent,
            status = status
        )
    }
}
