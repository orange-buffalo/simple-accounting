package io.orangebuffalo.simpleaccounting.infra.rest.filtering

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.getServerWebExchange
import org.jooq.DSLContext
import org.jooq.Table
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

class FilteringApiExecutorLegacy<E : Any, DTO : Any>(
    private val queryExecutor: FilteringApiQueryExecutorLegacy<*, E>,
    private val apiRequestResolver: FilteringApiRequestResolver,
    private val workspacesService: WorkspacesService,
    private val mapper: suspend E.() -> DTO
) {
    suspend fun executeFiltering(
        workspaceId: Long,
        mode: WorkspaceAccessMode = WorkspaceAccessMode.READ_ONLY
    ): ApiPage<DTO> {
        val workspace = workspacesService.getAccessibleWorkspace(workspaceId, mode)
        val filteringApiRequest = apiRequestResolver.resolveRequest(getServerWebExchange())
        val entityPage = queryExecutor.executeFilteringQuery(filteringApiRequest, workspace.id)
        return ApiPage(
            pageNumber = entityPage.pageNumber,
            pageSize = entityPage.pageSize,
            totalElements = entityPage.totalElements,
            data = entityPage.data.map { entity -> mapper(entity) }
        )
    }

    suspend fun executeFiltering(): ApiPage<DTO> {
        val filteringApiRequest = apiRequestResolver.resolveRequest(getServerWebExchange())
        val entityPage = queryExecutor.executeFilteringQuery(filteringApiRequest)
        return ApiPage(
            pageNumber = entityPage.pageNumber,
            pageSize = entityPage.pageSize,
            totalElements = entityPage.totalElements,
            data = entityPage.data.map { entity -> mapper(entity) }
        )
    }
}

@Component
@Deprecated("Define filter parameters explicitly for proper schema generation, use FilteringApiExecutorBuilder")
class FilteringApiExecutorBuilderLegacy(
    private val dslContext: DSLContext,
    private val conversionService: ConversionService,
    private val apiRequestResolver: FilteringApiRequestResolver,
    private val workspacesService: WorkspacesService
) {
    final inline fun <reified E : Any, DTO : Any> executor(
        noinline spec: ExecutorConfig<E, DTO>.() -> Unit
    ): FilteringApiExecutorLegacy<E, DTO> = executor(E::class, spec)

    fun <E : Any, DTO : Any> executor(
        entityType: KClass<E>,
        spec: ExecutorConfig<E, DTO>.() -> Unit
    ): FilteringApiExecutorLegacy<E, DTO> {
        val config = ExecutorConfig<E, DTO>(entityType)
        spec(config)
        return config.createExecutor()
    }

    inner class ExecutorConfig<E : Any, DTO : Any>(private val entityType: KClass<E>) {
        private lateinit var queryExecutor: FilteringApiQueryExecutorLegacy<*, E>
        private lateinit var mapper: suspend E.() -> DTO

        fun mapper(spec: suspend E.() -> DTO) {
            this.mapper = spec
        }

        fun <T : Table<*>> query(root: T, spec: FilteringApiQuerySpecLegacy<T>.() -> Unit) {
            queryExecutor = FilteringApiQueryExecutorLegacy(
                dslContext = dslContext,
                conversionService = conversionService,
                root = root,
                init = spec,
                entityType = entityType
            )
        }

        internal fun createExecutor(): FilteringApiExecutorLegacy<E, DTO> {
            return FilteringApiExecutorLegacy(
                queryExecutor = queryExecutor,
                workspacesService = workspacesService,
                apiRequestResolver = apiRequestResolver,
                mapper = mapper
            )
        }
    }
}
