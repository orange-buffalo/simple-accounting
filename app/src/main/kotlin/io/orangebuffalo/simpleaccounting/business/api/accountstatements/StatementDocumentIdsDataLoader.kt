package io.orangebuffalo.simpleaccounting.business.api.accountstatements

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.infra.graphql.newMappedDataLoader
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.dataloader.DataLoader
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceStatementKey(val workspaceId: String, val statementId: String)

private const val NAME = "statementDocumentIds"

@Component
class StatementDocumentIdsDataLoader(private val dsl: DSLContext) : KotlinDataLoader<WorkspaceStatementKey, List<String>> {
    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceStatementKey, List<String>> =
        newMappedDataLoader { keys ->
            val statement = Tables.ACCOUNT_STATEMENT_RECORD
            val attachment = Tables.ACCOUNT_STATEMENT_ATTACHMENTS
            val ids = dsl.select(statement.workspaceId, statement.id, attachment.documentId)
                .from(statement).join(attachment).on(attachment.accountStatementId.eq(statement.id))
                .where(statement.id.`in`(keys.map { it.statementId }))
                .and(statement.workspaceId.`in`(keys.map { it.workspaceId }))
                .orderBy(attachment.documentId)
                .fetch().groupBy(
                    keySelector = { WorkspaceStatementKey(workspaceId = it[statement.workspaceId]!!, statementId = it[statement.id]!!) },
                    valueTransform = { it[attachment.documentId]!! },
                )
            keys.associateWith { ids[it].orEmpty() }
        }
}

fun DataFetchingEnvironment.loadStatementDocumentIds(workspaceId: String, statementId: String): CompletableFuture<List<String>> =
    getDataLoader<WorkspaceStatementKey, List<String>>(NAME)!!
        .load(WorkspaceStatementKey(workspaceId = workspaceId, statementId = statementId))
