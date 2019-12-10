package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.CustomerService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Customer
import io.orangebuffalo.simpleaccounting.services.persistence.entities.QCustomer
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.simpleaccounting.web.api.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.hibernate.validator.constraints.Length
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/customers")
class CustomersApiController(
    private val customerService: CustomerService,
    private val workspaceService: WorkspaceService
) {

    @PostMapping
    suspend fun createCustomer(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditCustomerDto
    ): CustomerDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        return customerService
            .saveCustomer(
                Customer(
                    name = request.name,
                    workspace = workspace
                )
            )
            .let(::mapCustomerDto)
    }

    @GetMapping
    @PageableApi(CustomerPageableApiDescriptor::class)
    suspend fun getCustomers(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Page<Customer> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        return customerService.getCustomers(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{customerId}")
    suspend fun getCustomer(
        @PathVariable workspaceId: Long,
        @PathVariable customerId: Long
    ): CustomerDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expense = customerService.getCustomerByIdAndWorkspace(customerId, workspace)
            ?: throw EntityNotFoundException("Customer $customerId is not found")
        return mapCustomerDto(expense)
    }

    @PutMapping("{customerId}")
    suspend fun updateCustomer(
        @PathVariable workspaceId: Long,
        @PathVariable customerId: Long,
        @RequestBody @Valid request: EditCustomerDto
    ): CustomerDto {

        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)

        // todo #71: optimistic locking. etag?
        val customer = customerService.getCustomerByIdAndWorkspace(customerId, workspace)
            ?: throw EntityNotFoundException("Customer $customerId is not found")

        return customer
            .apply {
                name = request.name
            }
            .let {
                customerService.saveCustomer(it)
            }
            .let {
                mapCustomerDto(it)
            }
    }
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

private fun mapCustomerDto(source: Customer) = CustomerDto(
    name = source.name,
    id = source.id!!,
    version = source.version
)

@Component
class CustomerPageableApiDescriptor : PageableApiDescriptor<Customer, QCustomer> {
    override suspend fun mapEntityToDto(entity: Customer) =
        mapCustomerDto(entity)
}
