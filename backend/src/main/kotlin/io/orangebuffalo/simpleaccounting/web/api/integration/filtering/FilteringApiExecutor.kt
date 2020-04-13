package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.getServerWebExchange
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiPage
import org.jooq.DSLContext
import org.jooq.Table
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

class FilteringApiExecutor<T : Table<*>, E : Any, DTO : Any>(
    private val queryExecutor: FilteringApiQueryExecutor<T, E>,
    private val apiRequestResolver: FilteringApiRequestResolver,
    private val workspaceService: WorkspaceService,
    private val mapper: E.() -> DTO
) {
    suspend fun executeFiltering(workspaceId: Long): ApiPage<DTO> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        // todo workspace filtering
        val filteringApiRequest = apiRequestResolver.resolveRequest(getServerWebExchange())
        val entityPage = queryExecutor.executeFilteringQuery(filteringApiRequest)
        return ApiPage(
            pageNumber = entityPage.pageNumber,
            pageSize = entityPage.pageSize,
            totalElements = entityPage.totalElements,
            data = entityPage.data.map(mapper)
        )
    }
}

@Component
class FilteringApiExecutorBuilder(
    private val dslContext: DSLContext,
    private val conversionService: ConversionService,
    private val apiRequestResolver: FilteringApiRequestResolver,
    private val workspaceService: WorkspaceService
) {
    final inline fun <T : Table<*>, reified E : Any, DTO : Any> executor(
        noinline spec: ExecutorConfig<T, E, DTO>.() -> Unit
    ): FilteringApiExecutor<T, E, DTO> = executor(E::class, spec)

    fun <T : Table<*>, E : Any, DTO : Any> executor(
        entityType: KClass<E>,
        spec: ExecutorConfig<T, E, DTO>.() -> Unit
    ): FilteringApiExecutor<T, E, DTO> {
        val config = ExecutorConfig<T, E, DTO>(entityType)
        spec(config)
        return config.createExecutor()
    }

    inner class ExecutorConfig<T : Table<*>, E : Any, DTO : Any>(private val entityType: KClass<E>) {
        private lateinit var queryExecutor: FilteringApiQueryExecutor<T, E>
        private lateinit var mapper: E.() -> DTO

        fun mapper(spec: E.() -> DTO) {
            this.mapper = spec
        }

        fun query(root: T, spec: FilteringApiQuerySpec<T>.() -> Unit) {
            queryExecutor = FilteringApiQueryExecutor(
                dslContext = dslContext,
                conversionService = conversionService,
                root = root,
                init = spec,
                entityType = entityType
            )
        }

        internal fun createExecutor(): FilteringApiExecutor<T, E, DTO> {
            return FilteringApiExecutor(
                queryExecutor = queryExecutor,
                workspaceService = workspaceService,
                apiRequestResolver = apiRequestResolver,
                mapper = mapper
            )
        }
    }
}
