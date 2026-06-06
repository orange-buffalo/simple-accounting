package io.orangebuffalo.simpleaccounting.business.api.customers

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.customers.CustomersRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceCustomerKey(val workspaceId: String, val customerId: String)

private const val NAME = "customerByWorkspaceAndId"

@Component
class CustomerByWorkspaceAndIdDataLoader(
    private val customersRepository: CustomersRepository,
) : KotlinDataLoader<WorkspaceCustomerKey, CustomerGqlDto> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceCustomerKey, CustomerGqlDto> =
        newAsyncMappedDataLoader { keys ->
            val customerIds = keys.map { it.customerId }.toSet()
            val customers = customersRepository.findAllById(customerIds)
            customers.associate { customer ->
                WorkspaceCustomerKey(customer.workspaceId, customer.id!!) to CustomerGqlDto(
                    id = customer.id!!,
                    name = customer.name,
                )
            }
        }
}

fun DataFetchingEnvironment.loadCustomerByWorkspaceAndId(
    workspaceId: String,
    customerId: String,
): CompletableFuture<CustomerGqlDto?> =
    getDataLoader<WorkspaceCustomerKey, CustomerGqlDto>(NAME)!!.load(WorkspaceCustomerKey(workspaceId, customerId)).thenApply { it }
