package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.accounting.simpleaccounting.services.business.CustomerService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Customer
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QCustomer
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
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("/api/v1/user/workspaces/{workspaceId}/customers")
class CustomerApiController(
    private val extensions: ApiControllersExtensions,
    private val customerService: CustomerService
) {

    @PostMapping
    fun createCustomer(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditCustomerDto
    ): Mono<CustomerDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId)

        customerService.saveCustomer(
            Customer(
                name = request.name,
                workspace = workspace
            )
        ).let(::mapCustomerDto)
    }

    @GetMapping
    @PageableApi(CustomerPageableApiDescriptor::class)
    fun getCustomers(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<Customer>> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        customerService.getCustomers(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{customerId}")
    fun getCustomer(
        @PathVariable workspaceId: Long,
        @PathVariable customerId: Long
    ): Mono<CustomerDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val expense = customerService.getCustomerByIdAndWorkspace(customerId, workspace)
            ?: throw EntityNotFoundException("Customer $customerId is not found")
        mapCustomerDto(expense)
    }

    @PutMapping("{customerId}")
    fun updateCustomer(
        @PathVariable workspaceId: Long,
        @PathVariable customerId: Long,
        @RequestBody @Valid request: EditCustomerDto
    ): Mono<CustomerDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId)

        // todo optimistic locking. etag?
        val customer = customerService.getCustomerByIdAndWorkspace(customerId, workspace)
            ?: throw EntityNotFoundException("Customer $customerId is not found")

        customer.apply {
            name = request.name
        }.let {
            customerService.saveCustomer(it)
        }.let {
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
    override suspend fun mapEntityToDto(entity: Customer) = mapCustomerDto(entity)
}