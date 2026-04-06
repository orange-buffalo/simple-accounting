package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadCategoryById
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadCategoryByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadCustomerByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadExpensesByWorkspaceId
import io.orangebuffalo.simpleaccounting.business.api.dataloaders.loadGeneralTaxByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.infra.graphql.getBean
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
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
        val workspace = Tables.WORKSPACE
        return paginationService.forTable(workspace)
            .applyCurrentUserFiltering { user -> workspace.ownerId.eq(user.id) }
            .page(first, after) { record ->
                WorkspaceGqlDto(
                    id = record[workspace.id]!!,
                    name = record[workspace.name]!!,
                    defaultCurrency = record[workspace.defaultCurrency]!!,
                )
            }
    }

    @Suppress("unused")
    @GraphQLDescription("Returns a workspace by its ID, if accessible by the current user.")
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_ACTOR)
    suspend fun workspace(
        @GraphQLDescription("ID of the workspace.") id: Long,
    ): WorkspaceGqlDto {
        val workspace = workspacesService.getAccessibleWorkspace(id, WorkspaceAccessMode.READ_ONLY)
        return WorkspaceGqlDto(
            id = workspace.id!!,
            name = workspace.name,
            defaultCurrency = workspace.defaultCurrency,
        )
    }
}

@GraphQLName("Workspace")
@GraphQLDescription("Workspace of a user.")
data class WorkspaceGqlDto(
    @GraphQLDescription("ID of the workspace.")
    val id: Long,

    @GraphQLDescription("Name of the workspace.")
    val name: String,

    @GraphQLDescription("Default currency of the workspace.")
    val defaultCurrency: String,
) {
    @GraphQLDescription("Categories in this workspace with cursor-based pagination.")
    suspend fun categories(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<CategoryGqlDto> {
        val categoryTable = Tables.CATEGORY
        return env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(categoryTable)
            .addPredicate(categoryTable.workspaceId.eq(id))
            .page(first, after) { record ->
                CategoryGqlDto(
                    id = record[categoryTable.id]!!,
                    name = record[categoryTable.name]!!,
                    description = record[categoryTable.description],
                    income = record[categoryTable.income]!!,
                    expense = record[categoryTable.expense]!!,
                )
            }
    }

    @GraphQLDescription("Returns a category by its ID if it belongs to this workspace, or null if not found.")
    fun category(
        @GraphQLDescription("ID of the category.") id: Long,
        env: DataFetchingEnvironment,
    ) = env.loadCategoryByWorkspaceAndId(workspaceId = this.id, categoryId = id)

    @GraphQLDescription("Expenses in this workspace.")
    fun expenses(env: DataFetchingEnvironment) = env.loadExpensesByWorkspaceId(id)

    @GraphQLDescription("Documents in this workspace with cursor-based pagination.")
    suspend fun documents(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<DocumentGqlDto> {
        val document = Tables.DOCUMENT
        val paginationService = env.graphQlContext.getBean<GraphqlPaginationService>()
        val documentsRepository = env.graphQlContext.getBean<DocumentsRepository>()
        return paginationService.forTable(document)
            .addPredicate(document.workspaceId.eq(id))
            .page(
                first = first,
                after = after,
                mapQueryRecord = { record ->
                    DocumentGqlDto(
                        id = record[document.id]!!,
                        version = record[document.version]!!,
                        name = record[document.name]!!,
                        timeUploaded = record[document.timeUploaded]!!,
                        sizeInBytes = record[document.sizeInBytes],
                        storageId = record[document.storageId]!!,
                        mimeType = record[document.mimeType]!!,
                        usedBy = emptyList(),
                    )
                },
                postProcess = { records ->
                    val usagesByDocId = documentsRepository.findUsagesByDocumentIds(records.map { it.id })
                    records.map { item -> item.copy(usedBy = usagesByDocId[item.id] ?: emptyList()) }
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
    ): ConnectionGqlDto<CustomerGqlDto> {
        val customer = Tables.CUSTOMER
        return env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(customer)
            .addPredicate(customer.workspaceId.eq(id))
            .page(first, after) { record ->
                CustomerGqlDto(
                    id = record[customer.id]!!,
                    name = record[customer.name]!!,
                )
            }
    }

    @GraphQLDescription("Returns a customer by its ID if it belongs to this workspace, or null if not found.")
    fun customer(
        @GraphQLDescription("ID of the customer.") id: Long,
        env: DataFetchingEnvironment,
    ) = env.loadCustomerByWorkspaceAndId(workspaceId = this.id, customerId = id)

    @Suppress("unused")
    @GraphQLDescription("Income tax payments in this workspace with cursor-based pagination.")
    suspend fun incomeTaxPayments(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<IncomeTaxPaymentGqlDto> {
        val incomeTaxPayment = Tables.INCOME_TAX_PAYMENT
        val attachmentsTable = Tables.INCOME_TAX_PAYMENT_ATTACHMENTS
        val dslContext = env.graphQlContext.getBean<DSLContext>()
        return env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(incomeTaxPayment)
            .addPredicate(incomeTaxPayment.workspaceId.eq(id))
            .page(
                first = first,
                after = after,
                mapQueryRecord = { record ->
                    IncomeTaxPaymentGqlDto(
                        id = record[incomeTaxPayment.id]!!,
                        title = record[incomeTaxPayment.title]!!,
                        datePaid = record[incomeTaxPayment.datePaid]!!,
                        reportingDate = record[incomeTaxPayment.reportingDate]!!,
                        amount = record[incomeTaxPayment.amount]!!,
                        notes = record[incomeTaxPayment.notes],
                        attachments = emptyList(),
                    )
                },
                postProcess = { records ->
                    val attachmentsByPaymentId = dslContext
                        .select(attachmentsTable.incomeTaxPaymentId, attachmentsTable.documentId)
                        .from(attachmentsTable)
                        .where(attachmentsTable.incomeTaxPaymentId.`in`(records.map { it.id }))
                        .fetch()
                        .groupBy(
                            { it[attachmentsTable.incomeTaxPaymentId]!! },
                            { it[attachmentsTable.documentId]!! },
                        )
                    records.map { dto ->
                        dto.copy(attachments = attachmentsByPaymentId[dto.id] ?: emptyList())
                    }
                },
            )
    }

    @Suppress("unused")
    @GraphQLDescription("General taxes in this workspace with cursor-based pagination.")
    suspend fun generalTaxes(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<GeneralTaxGqlDto> {
        val generalTax = Tables.GENERAL_TAX
        return env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(generalTax)
            .addPredicate(generalTax.workspaceId.eq(id))
            .page(first, after) { record ->
                GeneralTaxGqlDto(
                    id = record[generalTax.id]!!,
                    title = record[generalTax.title]!!,
                    description = record[generalTax.description],
                    rateInBps = record[generalTax.rateInBps]!!,
                )
            }
    }

    @GraphQLDescription("Returns a general tax by its ID if it belongs to this workspace, or null if not found.")
    fun generalTax(
        @GraphQLDescription("ID of the general tax.") id: Long,
        env: DataFetchingEnvironment,
    ) = env.loadGeneralTaxByWorkspaceAndId(workspaceId = this.id, taxId = id)
}

@GraphQLName("Category")
@GraphQLDescription("Category of incomes or expenses.")
data class CategoryGqlDto(
    @GraphQLDescription("ID of the category.")
    val id: Long,

    @GraphQLDescription("Name of the category.")
    val name: String,

    @GraphQLDescription("Description of the category.")
    val description: String?,

    @GraphQLDescription("Whether this category is used for incomes.")
    val income: Boolean,

    @GraphQLDescription("Whether this category is used for expenses.")
    val expense: Boolean,
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
