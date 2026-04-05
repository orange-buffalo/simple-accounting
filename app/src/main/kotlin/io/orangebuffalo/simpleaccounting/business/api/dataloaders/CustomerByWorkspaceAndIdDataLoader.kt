package io.orangebuffalo.simpleaccounting.business.api.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.CustomerGqlDto
import io.orangebuffalo.simpleaccounting.business.customers.CustomersRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceCustomerKey(val workspaceId: Long, val customerId: Long)

private const val NAME = "customerByWorkspaceAndId"

@Component
class CustomerByWorkspaceAndIdDataLoader(
    private val customersRepository: CustomersRepository,
) : KotlinDataLoader<WorkspaceCustomerKey, CustomerGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceCustomerKey, CustomerGqlDto?> =
        newAsyncMappedDataLoader { keys ->
            val customerIds = keys.map { it.customerId }.toSet()
            val customers = customersRepository.findAllById(customerIds)
            val customerMap = customers.associateBy { WorkspaceCustomerKey(it.workspaceId, it.id!!) }
            keys.associateWith { key ->
                customerMap[key]?.let { customer ->
                    CustomerGqlDto(
                        id = customer.id!!,
                        name = customer.name,
                    )
                }
            }
        }
}

fun DataFetchingEnvironment.loadCustomerByWorkspaceAndId(
    workspaceId: Long,
    customerId: Long,
): CompletableFuture<CustomerGqlDto?> =
    getDataLoader<WorkspaceCustomerKey, CustomerGqlDto?>(NAME)!!.load(WorkspaceCustomerKey(workspaceId, customerId))
