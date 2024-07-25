package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.executeInParallel
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomeTaxPaymentService(
    private val taxPaymentRepository: IncomeTaxPaymentsRepository,
    private val workspacesService: WorkspacesService,
    private val documentsService: DocumentsService
) {

    suspend fun saveTaxPayment(taxPayment: IncomeTaxPayment): IncomeTaxPayment {
        validateTaxPayment(taxPayment)
        return withDbContext { taxPaymentRepository.save(taxPayment) }
    }

    private suspend fun validateTaxPayment(taxPayment: IncomeTaxPayment) {
        executeInParallel {
            step { workspacesService.validateWorkspaceAccess(taxPayment.workspaceId, WorkspaceAccessMode.READ_WRITE) }
            step { validateAttachments(taxPayment) }
        }
    }

    private suspend fun validateAttachments(taxPayment: IncomeTaxPayment) {
        val attachmentsIds = taxPayment.attachments.map { it.documentId }
        documentsService.validateDocuments(taxPayment.workspaceId, attachmentsIds)
    }

    suspend fun getTaxPaymentByIdAndWorkspace(id: Long, workspaceId: Long): IncomeTaxPayment? = withDbContext {
        taxPaymentRepository.findByIdAndWorkspaceId(id, workspaceId)
    }

    suspend fun getTaxPaymentStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): IncomeTaxPaymentsStatistics = withDbContext {
        taxPaymentRepository.getTaxPaymentsStatistics(fromDate, toDate, workspaceId)
    }
}
