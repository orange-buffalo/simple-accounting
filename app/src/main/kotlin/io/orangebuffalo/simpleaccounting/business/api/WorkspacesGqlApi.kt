package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadCategoriesByWorkspaceId
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadCategoryById
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadExpensesByWorkspaceId
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.infra.graphql.getBean
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Component
@Validated
class WorkspacesQuery(
    private val workspacesService: WorkspacesService,
    private val paginationService: GraphqlPaginationService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns all workspaces accessible by the current user with cursor-based pagination.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun workspaces(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
    ): ConnectionGqlDto<WorkspaceGqlDto> {
        val principal = ensureRegularUserPrincipal()
        val workspace = Tables.WORKSPACE
        val owner = Tables.PLATFORM_USER
        return withDbContext {
            paginationService.forTable(workspace)
                .onQuery { query -> query.join(owner).on(owner.id.eq(workspace.ownerId)) }
                .addPredicate(owner.userName.eq(principal.userName))
                .page(first, after) { record ->
                    WorkspaceGqlDto(
                        id = record[workspace.id]!!.toInt(),
                        name = record[workspace.name]!!,
                        defaultCurrency = record[workspace.defaultCurrency]!!,
                    )
                }
        }
    }

    @Suppress("unused")
    @GraphQLDescription("Returns a workspace by its ID, if accessible by the current user.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_ACTOR)
    suspend fun workspace(
        @GraphQLDescription("ID of the workspace.") id: Int,
    ): WorkspaceGqlDto {
        val workspace = workspacesService.getAccessibleWorkspace(id.toLong(), WorkspaceAccessMode.READ_ONLY)
        return WorkspaceGqlDto(
            id = workspace.id!!.toInt(),
            name = workspace.name,
            defaultCurrency = workspace.defaultCurrency,
        )
    }
}

@GraphQLName("Workspace")
@GraphQLDescription("Workspace of a user.")
data class WorkspaceGqlDto(
    @GraphQLDescription("ID of the workspace.")
    val id: Int,

    @GraphQLDescription("Name of the workspace.")
    val name: String,

    @GraphQLDescription("Default currency of the workspace.")
    val defaultCurrency: String,
) {
    @GraphQLDescription("Categories in this workspace.")
    fun categories(env: DataFetchingEnvironment) = env.loadCategoriesByWorkspaceId(id.toLong())

    @GraphQLDescription("Expenses in this workspace.")
    fun expenses(env: DataFetchingEnvironment) = env.loadExpensesByWorkspaceId(id.toLong())

    @GraphQLDescription("Documents in this workspace with cursor-based pagination.")
    suspend fun documents(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<DocumentGqlDto> = withDbContext {
        val document = Tables.DOCUMENT
        val paginationService = env.graphQlContext.getBean<GraphqlPaginationService>()
        val documentsRepository = env.graphQlContext.getBean<DocumentsRepository>()
        paginationService.forTable(document)
            .addPredicate(document.workspaceId.eq(id.toLong()))
            .page(
                first = first,
                after = after,
                mapQueryRecord = { record ->
                    DocumentQueryRecord(
                        id = record[document.id]!!,
                        version = record[document.version]!!,
                        name = record[document.name]!!,
                        timeUploaded = record[document.timeUploaded]!!,
                        sizeInBytes = record[document.sizeInBytes],
                        storageId = record[document.storageId]!!,
                        mimeType = record[document.mimeType]!!,
                    )
                },
                postProcess = { records ->
                    val usagesByDocId = documentsRepository.findUsagesByDocumentIds(records.map { it.id })
                    records.map { item ->
                        DocumentGqlDto(
                            id = item.id.toInt(),
                            version = item.version,
                            name = item.name,
                            timeUploaded = item.timeUploaded.toString(),
                            sizeInBytes = item.sizeInBytes?.toInt(),
                            storageId = item.storageId,
                            mimeType = item.mimeType,
                            usedBy = usagesByDocId[item.id] ?: emptyList(),
                        )
                    }
                },
            )
    }

    @GraphQLDescription("Customers in this workspace with cursor-based pagination.")
    suspend fun customers(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<CustomerGqlDto> = withDbContext {
        val customer = Tables.CUSTOMER
        env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(customer)
            .addPredicate(customer.workspaceId.eq(id.toLong()))
            .page(first, after) { record ->
                CustomerGqlDto(
                    id = record[customer.id]!!.toInt(),
                    name = record[customer.name]!!,
                )
            }
    }
}

@GraphQLName("Category")
@GraphQLDescription("Category of incomes or expenses.")
data class CategoryGqlDto(
    @GraphQLDescription("Name of the category.")
    val name: String,
)

@GraphQLName("Expense")
@GraphQLDescription("Business expense.")
data class ExpenseGqlDto(
    @GraphQLDescription("Title of the expense.")
    val title: String,

    @property:GraphQLIgnore val categoryId: Long?,
) {
    @GraphQLDescription("Category of the expense.")
    fun category(env: DataFetchingEnvironment): CompletableFuture<CategoryGqlDto?>? {
        val catId = categoryId ?: return null
        return env.loadCategoryById(catId)
    }
}

private data class DocumentQueryRecord(
    val id: Long,
    val version: Int,
    val name: String,
    val timeUploaded: Instant,
    val sizeInBytes: Long?,
    val storageId: String,
    val mimeType: String,
)
