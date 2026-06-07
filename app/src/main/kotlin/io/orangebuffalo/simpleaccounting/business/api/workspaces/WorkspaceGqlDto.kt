package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.analytics.AnalyticsGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.CategoryTypeGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.loadCategoryByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.customers.CustomerGqlDto
import io.orangebuffalo.simpleaccounting.business.api.customers.loadCustomerByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentUsageFilterType
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentsGqlApi
import io.orangebuffalo.simpleaccounting.business.api.expenses.ExpenseGqlDto
import io.orangebuffalo.simpleaccounting.business.api.expenses.ExpensesGqlApi
import io.orangebuffalo.simpleaccounting.business.api.expenses.loadExpenseByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.incomes.IncomeGqlDto
import io.orangebuffalo.simpleaccounting.business.api.incomes.IncomesGqlApi
import io.orangebuffalo.simpleaccounting.business.api.invoices.InvoiceGqlDto
import io.orangebuffalo.simpleaccounting.business.api.invoices.InvoicesGqlApi
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.api.standalonedocuments.StandaloneDocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.standalonedocuments.StandaloneDocumentsGqlApi
import io.orangebuffalo.simpleaccounting.business.api.standalonedocuments.loadStandaloneDocumentByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.GeneralTaxGqlDto
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.loadGeneralTaxByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.incometaxpayments.IncomeTaxPaymentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.incometaxpayments.loadIncomeTaxPaymentByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.infra.graphql.getBean
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.util.concurrent.CompletableFuture

@GraphQLName("Workspace")
@GraphQLDescription("Workspace of a user.")
data class WorkspaceGqlDto(
    @GraphQLDescription("ID of the workspace.")
    val id: String,

    @GraphQLDescription("Version of the workspace state.")
    val version: Int,

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
        @GraphQLDescription("Optional free text search to filter categories by name.")
        freeSearchText: String? = null,
        @GraphQLDescription("Optional filter to include categories matching any of the specified usage types.")
        typeIn: List<CategoryTypeGqlDto>? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<CategoryGqlDto> {
        val categoryTable = Tables.CATEGORY
        return env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(categoryTable)
            .addPredicate(categoryTable.workspaceId.eq(id))
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(categoryTable.name.containsIgnoreCase(freeSearchText))
                }
                if (!typeIn.isNullOrEmpty()) {
                    it.addPredicate(
                        DSL.or(typeIn.map { type ->
                            when (type) {
                                CategoryTypeGqlDto.INCOME -> categoryTable.income.isTrue
                                CategoryTypeGqlDto.EXPENSE -> categoryTable.expense.isTrue
                            }
                        })
                    )
                }
            }
            .page(
                first = first,
                after = after,
                sortFields = if (freeSearchText != null) listOf(categoryTable.name.asc(), categoryTable.id.asc()) else null,
            ) { record ->
                CategoryGqlDto(
                    id = record[categoryTable.id]!!,
                    version = record[categoryTable.version]!!,
                    name = record[categoryTable.name]!!,
                    description = record[categoryTable.description],
                    income = record[categoryTable.income]!!,
                    expense = record[categoryTable.expense]!!,
                )
            }
    }

    @GraphQLDescription("Returns a category by its ID if it belongs to this workspace, or null if not found.")
    fun category(
        @GraphQLDescription("ID of the category.") id: String,
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
        @GraphQLDescription("ID of the expense.") id: String,
        env: DataFetchingEnvironment,
    ) = env.loadExpenseByWorkspaceAndId(workspaceId = this.id, expenseId = id)

    @Suppress("unused")
    @GraphQLDescription("Incomes in this workspace with cursor-based pagination.")
    suspend fun incomes(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        @GraphQLDescription("Optional free text search to filter incomes by title, notes, or category name.")
        freeSearchText: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<IncomeGqlDto> {
        return env.graphQlContext.getBean<IncomesGqlApi>()
            .loadIncomes(workspaceId = id, first = first, after = after, freeSearchText = freeSearchText)
    }

    @GraphQLDescription("Returns an income by its ID if it belongs to this workspace, or null if not found.")
    suspend fun income(
        @GraphQLDescription("ID of the income.") id: String,
        env: DataFetchingEnvironment,
    ): IncomeGqlDto? {
        return env.graphQlContext.getBean<IncomesGqlApi>()
            .loadIncome(workspaceId = this.id, incomeId = id)
    }

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
        @GraphQLDescription("Optional filter to include only invoices with the specified statuses.")
        statusIn: List<InvoiceStatus>? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<InvoiceGqlDto> {
        return env.graphQlContext.getBean<InvoicesGqlApi>()
            .loadInvoices(workspaceId = id, first = first, after = after, freeSearchText = freeSearchText, statusIn = statusIn)
    }

    @GraphQLDescription("Returns an invoice by its ID if it belongs to this workspace, or null if not found.")
    suspend fun invoice(
        @GraphQLDescription("ID of the invoice.") id: String,
        env: DataFetchingEnvironment,
    ): InvoiceGqlDto? {
        return env.graphQlContext.getBean<InvoicesGqlApi>()
            .loadInvoice(workspaceId = this.id, invoiceId = id)
    }

    @GraphQLDescription("Documents in this workspace with cursor-based pagination.")
    suspend fun documents(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        @GraphQLDescription("Optional free text search to filter documents by file name or usage title.")
        freeSearchText: String? = null,
        @GraphQLDescription("Optional filter to include only documents stored in any of the specified storages.")
        storageIdsIn: List<String>? = null,
        @GraphQLDescription("Optional filter to include only documents matching any of the specified usage states.")
        usageTypeIn: List<DocumentUsageFilterType>? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<DocumentGqlDto> {
        return env.graphQlContext.getBean<DocumentsGqlApi>()
            .loadDocuments(
                workspaceId = id,
                first = first,
                after = after,
                freeSearchText = freeSearchText,
                storageIdsIn = storageIdsIn,
                usageTypeIn = usageTypeIn,
            )
    }

    @GraphQLDescription("Customers in this workspace with cursor-based pagination.")
    suspend fun customers(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        @GraphQLDescription("Optional free text search to filter customers by name.")
        freeSearchText: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<CustomerGqlDto> {
        val customer = Tables.CUSTOMER
        return env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(customer)
            .addPredicate(customer.workspaceId.eq(id))
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(customer.name.containsIgnoreCase(freeSearchText))
                }
            }
            .page(
                first = first,
                after = after,
                sortFields = if (freeSearchText != null) listOf(customer.name.asc(), customer.id.asc()) else null,
            ) { record ->
                CustomerGqlDto(
                    id = record[customer.id]!!,
                    version = record[customer.version]!!,
                    name = record[customer.name]!!,
                )
            }
    }

    @GraphQLDescription("Returns a customer by its ID if it belongs to this workspace, or null if not found.")
    fun customer(
        @GraphQLDescription("ID of the customer.") id: String,
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
                        version = record[incomeTaxPayment.version]!!,
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
        @GraphQLDescription("ID of the income tax payment.") id: String,
        env: DataFetchingEnvironment,
    ) = env.loadIncomeTaxPaymentByWorkspaceAndId(workspaceId = this.id, paymentId = id)

    @Suppress("unused")
    @GraphQLDescription("Standalone documents in this workspace with cursor-based pagination.")
    suspend fun standaloneDocuments(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<StandaloneDocumentGqlDto> {
        return env.graphQlContext.getBean<StandaloneDocumentsGqlApi>()
            .loadStandaloneDocuments(workspaceId = id, first = first, after = after)
    }

    @GraphQLDescription("Returns a standalone document by its ID if it belongs to this workspace, or null if not found.")
    fun standaloneDocument(
        @GraphQLDescription("ID of the standalone document.") id: String,
        env: DataFetchingEnvironment,
    ): CompletableFuture<StandaloneDocumentGqlDto?> =
        env.loadStandaloneDocumentByWorkspaceAndId(workspaceId = this.id, standaloneDocumentId = id)

    @Suppress("unused")
    @GraphQLDescription("General taxes in this workspace with cursor-based pagination.")
    suspend fun generalTaxes(
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
        @GraphQLDescription("Optional free text search to filter general taxes by title.")
        freeSearchText: String? = null,
        env: DataFetchingEnvironment,
    ): ConnectionGqlDto<GeneralTaxGqlDto> {
        val generalTax = Tables.GENERAL_TAX
        return env.graphQlContext.getBean<GraphqlPaginationService>()
            .forTable(generalTax)
            .addPredicate(generalTax.workspaceId.eq(id))
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(generalTax.title.containsIgnoreCase(freeSearchText))
                }
            }
            .page(
                first = first,
                after = after,
                sortFields = if (freeSearchText != null) listOf(generalTax.title.asc(), generalTax.id.asc()) else null,
            ) { record ->
                GeneralTaxGqlDto(
                    id = record[generalTax.id]!!,
                    version = record[generalTax.version]!!,
                    title = record[generalTax.title]!!,
                    description = record[generalTax.description],
                    rateInBps = record[generalTax.rateInBps]!!,
                )
            }
    }

    @GraphQLDescription("Returns a general tax by its ID if it belongs to this workspace, or null if not found.")
    fun generalTax(
        @GraphQLDescription("ID of the general tax.") id: String,
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
    version = version!!,
    name = name,
    defaultCurrency = defaultCurrency,
)
