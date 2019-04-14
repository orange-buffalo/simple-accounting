package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.TaxReport
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.TaxReportingRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TaxReportingService(
    private val taxReportingRepository: TaxReportingRepository
) {

    suspend fun getTaxReport(fromDate: LocalDate, toDate: LocalDate, workspace: Workspace): TaxReport = withDbContext {
        taxReportingRepository.getTaxReport(fromDate, toDate, workspace)
    }
}

