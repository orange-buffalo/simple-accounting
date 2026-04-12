package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.analytics.AnalyticsGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.loadCategoryByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.customers.CustomerGqlDto
import io.orangebuffalo.simpleaccounting.business.api.customers.loadCustomerByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.expenses.ExpenseGqlDto
import io.orangebuffalo.simpleaccounting.business.api.expenses.ExpensesGqlApi
import io.orangebuffalo.simpleaccounting.business.api.expenses.loadExpenseByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.invoices.InvoiceGqlDto
import io.orangebuffalo.simpleaccounting.business.api.invoices.InvoicesGqlApi
import io.orangebuffalo.simpleaccounting.business.api.invoices.loadInvoiceByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.GeneralTaxGqlDto
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.loadGeneralTaxByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.incometaxpayments.IncomeTaxPaymentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.incometaxpayments.loadIncomeTaxPaymentByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.infra.graphql.getBean
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.jooq.DSLContext

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
    @GraphQLDescription("Analytics data for this workspace.")
    fun analytics() = AnalyticsGqlDto(workspaceId = id)

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

    @Suppress("unused")
    @GraphQLDescription("Expenses in this workspace with cursor-based pagination.")
    suspend fun expenses(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        @GraphQLDescription("Optional free text search to filter expenses by title, notes, or category name.")
        freeSearchText: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<ExpenseGqlDto> {
        return env.graphQlContext.getBean<ExpensesGqlApi>()
            .loadExpenses(workspaceId = id, first = first, after = after, freeSearchText = freeSearchText)
    }

    @GraphQLDescription("Returns an expense by its ID if it belongs to this workspace, or null if not found.")
    fun expense(
        @GraphQLDescription("ID of the expense.") id: Long,
        env: DataFetchingEnvironment,
    ) = env.loadExpenseByWorkspaceAndId(workspaceId = this.id, expenseId = id)

    @Suppress("unused")
    @GraphQLDescription("Invoices in this workspace with cursor-based pagination.")
    suspend fun invoices(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        @GraphQLDescription("Optional free text search to filter invoices by title, notes, or customer name.")
        freeSearchText: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<InvoiceGqlDto> {
        return env.graphQlContext.getBean<InvoicesGqlApi>()
            .loadInvoices(workspaceId = id, first = first, after = after, freeSearchText = freeSearchText)
    }

    @GraphQLDescription("Returns an invoice by its ID if it belongs to this workspace, or null if not found.")
    fun invoice(
        @GraphQLDescription("ID of the invoice.") id: Long,
        env: DataFetchingEnvironment,
    ) = env.loadInvoiceByWorkspaceAndId(workspaceId = this.id, invoiceId = id)

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
                        attachmentIds = emptyList(),
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
                        dto.copy(attachmentIds = attachmentsByPaymentId[dto.id] ?: emptyList())
                    }
                },
            )
    }

    @GraphQLDescription("Returns an income tax payment by its ID if it belongs to this workspace, or null if not found.")
    fun incomeTaxPayment(
        @GraphQLDescription("ID of the income tax payment.") id: Long,
        env: DataFetchingEnvironment,
    ) = env.loadIncomeTaxPaymentByWorkspaceAndId(workspaceId = this.id, paymentId = id)

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

    @Suppress("unused")
    @GraphQLDescription("Workspace access tokens in this workspace with cursor-based pagination.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun workspaceAccessTokens(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<WorkspaceAccessTokenGqlDto> {
        val workspaceAccessTokenTable = Tables.WORKSPACE_ACCESS_TOKEN
        return env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(workspaceAccessTokenTable)
            .addPredicate(workspaceAccessTokenTable.workspaceId.eq(id))
            .page(first, after) { record ->
                WorkspaceAccessTokenGqlDto(
                    id = record[workspaceAccessTokenTable.id]!!,
                    version = record[workspaceAccessTokenTable.version]!!,
                    validTill = record[workspaceAccessTokenTable.validTill]!!,
                    revoked = record[workspaceAccessTokenTable.revoked]!!,
                    token = record[workspaceAccessTokenTable.token]!!,
                )
            }
    }
}

internal fun io.orangebuffalo.simpleaccounting.business.workspaces.Workspace.toWorkspaceGqlDto() = WorkspaceGqlDto(
    id = id!!,
    name = name,
    defaultCurrency = defaultCurrency,
)
