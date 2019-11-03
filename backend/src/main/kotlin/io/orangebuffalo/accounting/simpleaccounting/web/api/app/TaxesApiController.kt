package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.accounting.simpleaccounting.services.business.GeneralTaxService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.GeneralTax
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QGeneralTax
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.hibernate.validator.constraints.Length
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/general-taxes")
class GeneralTaxApiController(
    private val taxService: GeneralTaxService,
    private val workspaceService: WorkspaceService
) {

    @PostMapping
    suspend fun createTax(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditGeneralTaxDto
    ): GeneralTaxDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        return taxService
            .saveTax(
                GeneralTax(
                    title = request.title,
                    description = request.description,
                    rateInBps = request.rateInBps,
                    workspace = workspace
                )
            )
            .let(::mapTaxDto)
    }

    @GetMapping
    @PageableApi(GeneralTaxPageableApiDescriptor::class)
    suspend fun getTaxes(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Page<GeneralTax> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        return taxService.getTaxes(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{taxId}")
    suspend fun getTax(
        @PathVariable workspaceId: Long,
        @PathVariable taxId: Long
    ): GeneralTaxDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expense = taxService.getTaxByIdAndWorkspace(taxId, workspace)
            ?: throw EntityNotFoundException("Tax $taxId is not found")
        return mapTaxDto(expense)
    }

    @PutMapping("{taxId}")
    suspend fun updateTax(
        @PathVariable workspaceId: Long,
        @PathVariable taxId: Long,
        @RequestBody @Valid request: EditGeneralTaxDto
    ): GeneralTaxDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val tax = taxService.getTaxByIdAndWorkspace(taxId, workspace)
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
    version = source.version,
    description = source.description,
    rateInBps = source.rateInBps
)

@Component
class GeneralTaxPageableApiDescriptor : PageableApiDescriptor<GeneralTax, QGeneralTax> {
    override suspend fun mapEntityToDto(entity: GeneralTax) = mapTaxDto(entity)
}
