package io.orangebuffalo.simpleaccounting.domain.customers

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.*
import org.hibernate.validator.constraints.Length
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/customers")
class CustomersApiController(
    private val customerService: CustomerService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @PostMapping
    suspend fun createCustomer(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditCustomerDto
    ): CustomerDto = customerService
        .saveCustomer(
            Customer(
                name = request.name,
                workspaceId = workspaceId
            )
        )
        .mapToCustomerDto()

    @GetMapping
    suspend fun getCustomers(
        @PathVariable workspaceId: Long,
        @ParameterObject request: CustomersFilteringRequest
    ): ApiPage<CustomerDto> = filteringApiExecutor.executeFiltering(request, workspaceId)

    @GetMapping("{customerId}")
    suspend fun getCustomer(
        @PathVariable workspaceId: Long,
        @PathVariable customerId: Long
    ): CustomerDto {
        workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val customer = customerService.getCustomerByIdAndWorkspace(customerId, workspaceId)
            ?: throw EntityNotFoundException("Customer $customerId is not found")
        return customer.mapToCustomerDto()
    }

    @PutMapping("{customerId}")
    suspend fun updateCustomer(
        @PathVariable workspaceId: Long,
        @PathVariable customerId: Long,
        @RequestBody @Valid request: EditCustomerDto
    ): CustomerDto {
        // todo #71: optimistic locking. etag?
        val customer = customerService.getCustomerByIdAndWorkspace(customerId, workspaceId)
            ?: throw EntityNotFoundException("Customer $customerId is not found")

        return customer
            .apply { name = request.name }
            .let { customerService.saveCustomer(it) }
            .mapToCustomerDto()
    }

    private val filteringApiExecutor =
        filteringApiExecutorBuilder.executor<Customer, CustomerDto, NoOpSorting, CustomersFilteringRequest> {
            query(Tables.CUSTOMER) {
                addDefaultSorting { root.id.desc() }
                workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
            }
            mapper { mapToCustomerDto() }
        }
}

class CustomersFilteringRequest : ApiPageRequest<NoOpSorting>() {
    override var sortBy: NoOpSorting? = null
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CustomerDto(
    val name: String,
    val id: Long,
    val version: Int
)

data class EditCustomerDto(
    @field:NotBlank @field:Length(max = 255) val name: String
)

private fun Customer.mapToCustomerDto() = CustomerDto(
    name = name,
    id = id!!,
    version = version!!
)
