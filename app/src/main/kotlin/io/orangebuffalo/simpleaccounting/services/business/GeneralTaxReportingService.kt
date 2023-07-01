package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.repos.GeneralTaxReport
import io.orangebuffalo.simpleaccounting.services.persistence.repos.GeneralTaxReportingRepository
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

