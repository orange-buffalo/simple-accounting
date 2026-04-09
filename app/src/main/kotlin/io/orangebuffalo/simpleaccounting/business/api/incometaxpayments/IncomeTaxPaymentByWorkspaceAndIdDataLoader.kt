package io.orangebuffalo.simpleaccounting.business.api.incometaxpayments

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentsRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceIncomeTaxPaymentKey(val workspaceId: Long, val paymentId: Long)

private const val NAME = "incomeTaxPaymentByWorkspaceAndId"

@Component
class IncomeTaxPaymentByWorkspaceAndIdDataLoader(
    private val incomeTaxPaymentsRepository: IncomeTaxPaymentsRepository,
) : KotlinDataLoader<WorkspaceIncomeTaxPaymentKey, IncomeTaxPaymentGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceIncomeTaxPaymentKey, IncomeTaxPaymentGqlDto?> =
        newAsyncMappedDataLoader { keys ->
            val paymentIds = keys.map { it.paymentId }.toSet()
            val payments = incomeTaxPaymentsRepository.findAllById(paymentIds)
            val paymentMap = payments.associateBy { WorkspaceIncomeTaxPaymentKey(it.workspaceId, it.id!!) }
            keys.associateWith { key ->
                paymentMap[key]?.let { payment ->
                    IncomeTaxPaymentGqlDto(
                        id = payment.id!!,
                        title = payment.title,
                        datePaid = payment.datePaid,
                        reportingDate = payment.reportingDate,
                        amount = payment.amount,
                        notes = payment.notes,
                        attachmentIds = payment.attachments.map { it.documentId },
                    )
                }
            }
        }
}

fun DataFetchingEnvironment.loadIncomeTaxPaymentByWorkspaceAndId(
    workspaceId: Long,
    paymentId: Long,
): CompletableFuture<IncomeTaxPaymentGqlDto?> =
    getDataLoader<WorkspaceIncomeTaxPaymentKey, IncomeTaxPaymentGqlDto?>(NAME)!!
        .load(WorkspaceIncomeTaxPaymentKey(workspaceId, paymentId))
