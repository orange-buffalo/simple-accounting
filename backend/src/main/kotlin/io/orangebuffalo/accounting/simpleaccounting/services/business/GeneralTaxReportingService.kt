package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.GeneralTaxReport
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.GeneralTaxReportingRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GeneralTaxReportingService(
    private val taxReportingRepository: GeneralTaxReportingRepository
) {

    suspend fun getGeneralTaxReport(fromDate: LocalDate, toDate: LocalDate, workspace: Workspace): GeneralTaxReport = withDbContext {
        taxReportingRepository.getGeneralTaxReport(fromDate, toDate, workspace)
    }
}

