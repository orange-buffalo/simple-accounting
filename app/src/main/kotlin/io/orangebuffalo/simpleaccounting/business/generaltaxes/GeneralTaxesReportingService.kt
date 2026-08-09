package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GeneralTaxesReportingService(
    private val reportingRepository: GeneralTaxesReportingRepository
) {

    fun getGeneralTaxReport(fromDate: LocalDate, toDate: LocalDate, workspace: Workspace): GeneralTaxReport =
        reportingRepository.getGeneralTaxReport(fromDate, toDate, workspace)
}
