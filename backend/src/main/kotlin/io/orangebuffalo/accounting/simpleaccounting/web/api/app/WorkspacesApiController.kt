package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.*
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/v1/user/workspaces")
class WorkspacesApiController(
    private val platformUserService: PlatformUserService,
    private val workspaceService: WorkspaceService,
    private val extensions: ApiControllersExtensions,
    private val expenseService: ExpenseService,
    private val incomeService: IncomeService
) {

    @GetMapping
    fun getWorkspaces(): Mono<List<WorkspaceDto>> = extensions.toMono {
        val userName = getCurrentPrincipal().username
        val workspaces = platformUserService.getUserWorkspacesAsync(userName)
        val categories = platformUserService.getUserCategoriesAsync(userName)

        workspaces.await()
            .map { workspace ->
                mapWorkspaceDto(
                    workspace,
                    //todo test for multiple workspace to verify await is fin to call multiple times
                    categories.await().asSequence()
                        .filter { category -> category.workspace == workspace }
                        .map(::mapCategoryDto)
                        .toList()
                )
            }
    }

    @PostMapping
    fun createWorkspace(
        @RequestBody @Valid createWorkspaceRequest: CreateWorkspaceDto
    ): Mono<WorkspaceDto> = extensions.toMono {
        platformUserService.createWorkspace(
            Workspace(
                name = createWorkspaceRequest.name,
                taxEnabled = false,
                multiCurrencyEnabled = true,
                defaultCurrency = createWorkspaceRequest.defaultCurrency,
                owner = platformUserService.getCurrentUser()
            )
        ).let { mapWorkspaceDto(it, emptyList()) }
    }

    @PutMapping("{workspaceId}")
    fun editWorkspace(
        @RequestBody @Valid editWorkspaceRequest: EditWorkspaceDto,
        @PathVariable workspaceId: Long
    ): Mono<WorkspaceDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        workspace.name = editWorkspaceRequest.name
        platformUserService.saveWorkspace(workspace)
        //todo remove categories from workspace dto
        mapWorkspaceDto(workspace, emptyList())
    }

    @PostMapping("/{workspaceId}/categories")
    fun createCategory(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid createCategoryRequest: CreateCategoryDto
    ): Mono<CategoryDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        workspaceService.createCategory(
            Category(
                name = createCategoryRequest.name,
                workspace = workspace,
                expense = createCategoryRequest.expense,
                income = createCategoryRequest.income,
                description = createCategoryRequest.description
            )
        ).let(::mapCategoryDto)
    }

    @GetMapping("/{workspaceId}/statistics/expenses")
    fun getExpensesStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): Mono<ExpensesStatisticsDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val expensesStatistics = expenseService.getExpensesStatistics(fromDate, toDate, workspace)
        ExpensesStatisticsDto(
            expensesStatistics.map {
                ExpensesStatisticsItemDto(
                    it.categoryId,
                    it.totalAmount,
                    it.finalizedCount,
                    it.pendingCount
                )
            }
        )
    }

    @GetMapping("/{workspaceId}/statistics/incomes")
    fun getIncomesStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): Mono<IncomesStatisticsDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val incomesStatistics = incomeService.getIncomesStatistics(fromDate, toDate, workspace)
        IncomesStatisticsDto(
            incomesStatistics.map {
                IncomesStatisticsItemDto(
                    it.categoryId,
                    it.totalAmount,
                    it.finalizedCount,
                    it.pendingCount,
                    it.currencyExchangeGain
                )
            }
        )
    }
}

@Suppress("unused")
data class ExpensesStatisticsDto(
    val items: List<ExpensesStatisticsItemDto>
) {
    val totalAmount: Long = items.map { it.totalAmount }.sum()
    val finalizedCount: Long = items.map { it.finalizedCount }.sum()
    val pendingCount: Long = items.map { it.pendingCount }.sum()
}

data class ExpensesStatisticsItemDto(
    val categoryId: Long,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long
)

@Suppress("unused")
data class IncomesStatisticsDto(
    val items: List<IncomesStatisticsItemDto>
) {
    val totalAmount: Long = items.map { it.totalAmount }.sum()
    val finalizedCount: Long = items.map { it.finalizedCount }.sum()
    val pendingCount: Long = items.map { it.pendingCount }.sum()
    val currencyExchangeGain = items.map { it.currencyExchangeGain }.sum()
}

data class IncomesStatisticsItemDto(
    val categoryId: Long,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,
    val currencyExchangeGain: Long
)

data class WorkspaceDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var taxEnabled: Boolean,
    var multiCurrencyEnabled: Boolean,
    var defaultCurrency: String,
    var categories: List<CategoryDto> = emptyList()
)

data class CategoryDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var description: String?,
    var income: Boolean,
    var expense: Boolean
)

data class CreateWorkspaceDto(
    @field:NotBlank var name: String,
    // todo multicurrency is probably redundant; tax to be enabled later
    //@field:NotNull var taxEnabled: Boolean,
    //@field:NotNull var multiCurrencyEnabled: Boolean,
    @field:NotBlank val defaultCurrency: String
)

data class EditWorkspaceDto(
    @field:NotBlank @field:Size(max = 255) val name: String
)

data class CreateCategoryDto(
    @field:NotBlank var name: String,
    var description: String?,
    @field:NotNull var income: Boolean,
    @field:NotNull var expense: Boolean
)

private fun mapWorkspaceDto(source: Workspace, categories: List<CategoryDto>): WorkspaceDto = WorkspaceDto(
    name = source.name,
    id = source.id,
    version = source.version,
    taxEnabled = source.taxEnabled,
    multiCurrencyEnabled = source.multiCurrencyEnabled,
    defaultCurrency = source.defaultCurrency,
    categories = categories
)

private fun mapCategoryDto(source: Category) = CategoryDto(
    name = source.name,
    id = source.id,
    version = source.version,
    description = source.description,
    income = source.income,
    expense = source.expense
)