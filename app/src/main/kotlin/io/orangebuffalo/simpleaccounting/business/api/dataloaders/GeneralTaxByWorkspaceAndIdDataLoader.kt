package io.orangebuffalo.simpleaccounting.business.api.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.GeneralTaxGqlDto
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceGeneralTaxKey(val workspaceId: Long, val taxId: Long)

private const val NAME = "generalTaxByWorkspaceAndId"

@Component
class GeneralTaxByWorkspaceAndIdDataLoader(
    private val generalTaxesRepository: GeneralTaxesRepository,
) : KotlinDataLoader<WorkspaceGeneralTaxKey, GeneralTaxGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceGeneralTaxKey, GeneralTaxGqlDto?> =
        newAsyncMappedDataLoader { keys ->
            val taxIds = keys.map { it.taxId }.toSet()
            val taxes = generalTaxesRepository.findAllById(taxIds)
            val taxMap = taxes.associateBy { WorkspaceGeneralTaxKey(it.workspaceId, it.id!!) }
            keys.associateWith { key ->
                taxMap[key]?.let { tax ->
                    GeneralTaxGqlDto(
                        id = tax.id!!,
                        title = tax.title,
                        description = tax.description,
                        rateInBps = tax.rateInBps,
                    )
                }
            }
        }
}

fun DataFetchingEnvironment.loadGeneralTaxByWorkspaceAndId(
    workspaceId: Long,
    taxId: Long,
): CompletableFuture<GeneralTaxGqlDto?> =
    getDataLoader<WorkspaceGeneralTaxKey, GeneralTaxGqlDto?>(NAME)!!.load(WorkspaceGeneralTaxKey(workspaceId, taxId))
