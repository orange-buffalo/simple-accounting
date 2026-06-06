package io.orangebuffalo.simpleaccounting.business.api.generaltaxes

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceGeneralTaxKey(val workspaceId: String, val taxId: String)

private const val NAME = "generalTaxByWorkspaceAndId"

@Component
class GeneralTaxByWorkspaceAndIdDataLoader(
    private val generalTaxesRepository: GeneralTaxesRepository,
) : KotlinDataLoader<WorkspaceGeneralTaxKey, GeneralTaxGqlDto> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceGeneralTaxKey, GeneralTaxGqlDto> =
        newAsyncMappedDataLoader { keys ->
            val taxIds = keys.map { it.taxId }.toSet()
            val taxes = generalTaxesRepository.findAllById(taxIds)
            taxes.associate { tax ->
                WorkspaceGeneralTaxKey(tax.workspaceId, tax.id!!) to GeneralTaxGqlDto(
                    id = tax.id!!,
                    title = tax.title,
                    description = tax.description,
                    rateInBps = tax.rateInBps,
                )
            }
        }
}

fun DataFetchingEnvironment.loadGeneralTaxByWorkspaceAndId(
    workspaceId: String,
    taxId: String,
): CompletableFuture<GeneralTaxGqlDto?> =
    getDataLoader<WorkspaceGeneralTaxKey, GeneralTaxGqlDto>(NAME)!!.load(WorkspaceGeneralTaxKey(workspaceId, taxId)).thenApply { it }
