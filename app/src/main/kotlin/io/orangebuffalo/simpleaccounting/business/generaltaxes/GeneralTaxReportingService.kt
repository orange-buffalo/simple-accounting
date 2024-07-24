package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
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

