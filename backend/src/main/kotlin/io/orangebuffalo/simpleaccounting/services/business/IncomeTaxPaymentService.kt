package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.services.integration.executeInParallel
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomeTaxPaymentRepository
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomeTaxPaymentsStatistics
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomeTaxPaymentService(
    private val taxPaymentRepository: IncomeTaxPaymentRepository,
    private val workspaceService: WorkspaceService,
    private val documentsService: DocumentsService
) {

    suspend fun saveTaxPayment(taxPayment: IncomeTaxPayment): IncomeTaxPayment {
        validateTaxPayment(taxPayment)
        return withDbContext { taxPaymentRepository.save(taxPayment) }
    }

    private suspend fun validateTaxPayment(taxPayment: IncomeTaxPayment) {
        executeInParallel {
            step { workspaceService.validateWorkspaceAccess(taxPayment.workspaceId, WorkspaceAccessMode.READ_WRITE) }
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
