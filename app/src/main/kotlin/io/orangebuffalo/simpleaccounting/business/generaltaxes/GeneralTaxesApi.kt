package io.orangebuffalo.simpleaccounting.business.generaltaxes

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.ApiPageRequest
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.NoOpSorting
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.hibernate.validator.constraints.Length
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/general-taxes")
class GeneralTaxesApi(
    private val taxService: GeneralTaxesService,
    private val workspacesService: WorkspacesService,
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
        .mapToTaxDto()

    @GetMapping
    suspend fun getTaxes(
        @PathVariable workspaceId: Long,
        @ParameterObject request: GeneralTaxFilteringRequest
    ): ApiPage<GeneralTaxDto> = filteringApiExecutor.executeFiltering(request, workspaceId)

    @GetMapping("{taxId}")
    suspend fun getTax(
        @PathVariable workspaceId: Long,
        @PathVariable taxId: Long
    ): GeneralTaxDto {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expense = taxService.getTaxByIdAndWorkspace(taxId, workspaceId)
            ?: throw EntityNotFoundException("Tax $taxId is not found")
        return expense.mapToTaxDto()
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
            .let { taxService.saveTax(it) }
            .mapToTaxDto()
    }

    private val filteringApiExecutor =
        filteringApiExecutorBuilder.executor<GeneralTax, GeneralTaxDto, NoOpSorting, GeneralTaxFilteringRequest> {
            query(Tables.GENERAL_TAX) {
                addDefaultSorting { root.id.desc() }
                workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
            }
            mapper { mapToTaxDto() }
        }
}

class GeneralTaxFilteringRequest : ApiPageRequest<NoOpSorting>() {
    override var sortBy: NoOpSorting? = null
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

private fun GeneralTax.mapToTaxDto() = GeneralTaxDto(
    title = title,
    id = id!!,
    version = version!!,
    description = description,
    rateInBps = rateInBps
)
