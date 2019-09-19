package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.accounting.simpleaccounting.services.business.TaxService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QTax
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Tax
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.hibernate.validator.constraints.Length
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/taxes")
class TaxApiController(
    private val extensions: ApiControllersExtensions,
    private val taxService: TaxService
) {

    @PostMapping
    fun createTax(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditTaxDto
    ): Mono<TaxDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        taxService.saveTax(
            Tax(
                title = request.title,
                description = request.description,
                rateInBps = request.rateInBps,
                workspace = workspace
            )
        ).let(::mapTaxDto)
    }

    @GetMapping
    @PageableApi(TaxPageableApiDescriptor::class)
    fun getTaxes(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<Tax>> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        taxService.getTaxes(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{taxId}")
    fun getTax(
        @PathVariable workspaceId: Long,
        @PathVariable taxId: Long
    ): Mono<TaxDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expense = taxService.getTaxByIdAndWorkspace(taxId, workspace)
            ?: throw EntityNotFoundException("Tax $taxId is not found")
        mapTaxDto(expense)
    }

    @PutMapping("{taxId}")
    fun updateTax(
        @PathVariable workspaceId: Long,
        @PathVariable taxId: Long,
        @RequestBody @Valid request: EditTaxDto
    ): Mono<TaxDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val tax = taxService.getTaxByIdAndWorkspace(taxId, workspace)
            ?: throw EntityNotFoundException("Tax $taxId is not found")

        tax.apply {
            title = request.title
            description = request.description
            rateInBps = request.rateInBps
        }.let {
            taxService.saveTax(it)
        }.let {
            mapTaxDto(it)
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TaxDto(
    val title: String,
    val id: Long,
    val version: Int,
    val description: String? = null,
    val rateInBps: Int
)

data class EditTaxDto(
    @field:NotBlank @field:Length(max = 255) val title: String,
    @field:Length(max = 255) val description: String? = null,
    @field:Min(0) @field:Max(100_00) val rateInBps: Int
)

private fun mapTaxDto(source: Tax) = TaxDto(
    title = source.title,
    id = source.id!!,
    version = source.version,
    description = source.description,
    rateInBps = source.rateInBps
)

@Component
class TaxPageableApiDescriptor : PageableApiDescriptor<Tax, QTax> {
    override suspend fun mapEntityToDto(entity: Tax) = mapTaxDto(entity)
}