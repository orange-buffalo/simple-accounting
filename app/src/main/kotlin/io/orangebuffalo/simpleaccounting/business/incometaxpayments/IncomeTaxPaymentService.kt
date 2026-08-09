package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomeTaxPaymentService(
    private val taxPaymentRepository: IncomeTaxPaymentsRepository,
    private val workspacesService: WorkspacesService,
    private val documentsService: DocumentsService
) {

    fun saveTaxPayment(taxPayment: IncomeTaxPayment): IncomeTaxPayment {
        validateTaxPayment(taxPayment)
        return taxPaymentRepository.save(taxPayment)
    }

    private fun validateTaxPayment(taxPayment: IncomeTaxPayment) {
        workspacesService.validateWorkspaceAccess(taxPayment.workspaceId, WorkspaceAccessMode.READ_WRITE)
        validateAttachments(taxPayment)
    }

    private fun validateAttachments(taxPayment: IncomeTaxPayment) {
        val attachmentsIds = taxPayment.attachments.map { it.documentId }
        documentsService.validateDocuments(taxPayment.workspaceId, attachmentsIds)
    }

    fun getTaxPaymentByIdAndWorkspace(id: String, workspaceId: String): IncomeTaxPayment? =
        taxPaymentRepository.findByIdAndWorkspaceId(id, workspaceId)

    fun getTaxPaymentStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: String
    ): IncomeTaxPaymentsStatistics = taxPaymentRepository.getTaxPaymentsStatistics(fromDate, toDate, workspaceId)
}
