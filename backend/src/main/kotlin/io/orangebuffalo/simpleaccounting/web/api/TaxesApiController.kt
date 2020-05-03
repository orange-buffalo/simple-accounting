package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.GeneralTaxService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.GeneralTax
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilder
import org.hibernate.validator.constraints.Length
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/general-taxes")
class GeneralTaxApiController(
    private val taxService: GeneralTaxService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @PostMapping
    suspend fun createTax(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditGeneralTaxDto
    ): GeneralTaxDto = taxService
        .saveTax(
            GeneralTax(
                title = request.title,
                description = request.description,
                rateInBps = request.rateInBps,
                workspaceId = workspaceId
            )
        )
        .let(::mapTaxDto)

    @GetMapping
    suspend fun getTaxes(@PathVariable workspaceId: Long): ApiPage<GeneralTaxDto> =
        filteringApiExecutor.executeFiltering(workspaceId)

    @GetMapping("{taxId}")
    suspend fun getTax(
        @PathVariable workspaceId: Long,
        @PathVariable taxId: Long
    ): GeneralTaxDto {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expense = taxService.getTaxByIdAndWorkspace(taxId, workspaceId)
            ?: throw EntityNotFoundException("Tax $taxId is not found")
        return mapTaxDto(expense)
    }

    @PutMapping("{taxId}")
    suspend fun updateTax(
        @PathVariable workspaceId: Long,
        @PathVariable taxId: Long,
        @RequestBody @Valid request: EditGeneralTaxDto
    ): GeneralTaxDto {

        // todo #71: optimistic locking. etag?
        val tax = taxService.getTaxByIdAndWorkspace(taxId, workspaceId)
            ?: throw EntityNotFoundException("Tax $taxId is not found")

        return tax
            .apply {
                title = request.title
                description = request.description
                rateInBps = request.rateInBps
            }
            .let {
                taxService.saveTax(it)
            }
            .let {
                mapTaxDto(it)
            }
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder.executor<GeneralTax, GeneralTaxDto> {
        query(Tables.GENERAL_TAX) {
            addDefaultSorting { root.id.desc() }
            workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
        }
        mapper { mapTaxDto(this) }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeneralTaxDto(
    val title: String,
    val id: Long,
    val version: Int,
    val description: String? = null,
    val rateInBps: Int
)

data class EditGeneralTaxDto(
    @field:NotBlank @field:Length(max = 255) val title: String,
    @field:Length(max = 255) val description: String? = null,
    @field:Min(0) @field:Max(100_00) val rateInBps: Int
)

private fun mapTaxDto(source: GeneralTax) = GeneralTaxDto(
    title = source.title,
    id = source.id!!,
    version = source.version!!,
    description = source.description,
    rateInBps = source.rateInBps
)
