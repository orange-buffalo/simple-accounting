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

class FilteringApiExecutor<E : Any, DTO : Any>(
    private val queryExecutor: FilteringApiQueryExecutor<*, E>,
    private val apiRequestResolver: FilteringApiRequestResolver,
    private val workspaceService: WorkspaceService,
    private val mapper: suspend E.() -> DTO
) {
    suspend fun executeFiltering(workspaceId: Long): ApiPage<DTO> {
        // todo #222: does not cover all cases, sometimes we need admin access
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val filteringApiRequest = apiRequestResolver.resolveRequest(getServerWebExchange())
        val entityPage = queryExecutor.executeFilteringQuery(filteringApiRequest, workspace.id)
        return ApiPage(
            pageNumber = entityPage.pageNumber,
            pageSize = entityPage.pageSize,
            totalElements = entityPage.totalElements,
            data = entityPage.data.map { entity -> mapper(entity) }
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
    final inline fun <reified E : Any, DTO : Any> executor(
        noinline spec: ExecutorConfig<E, DTO>.() -> Unit
    ): FilteringApiExecutor<E, DTO> = executor(E::class, spec)

    fun <E : Any, DTO : Any> executor(
        entityType: KClass<E>,
        spec: ExecutorConfig<E, DTO>.() -> Unit
    ): FilteringApiExecutor<E, DTO> {
        val config = ExecutorConfig<E, DTO>(entityType)
        spec(config)
        return config.createExecutor()
    }

    inner class ExecutorConfig<E : Any, DTO : Any>(private val entityType: KClass<E>) {
        private lateinit var queryExecutor: FilteringApiQueryExecutor<*, E>
        private lateinit var mapper: suspend E.() -> DTO

        fun mapper(spec: suspend E.() -> DTO) {
            this.mapper = spec
        }

        fun <T: Table<*>> query(root: T, spec: FilteringApiQuerySpec<T>.() -> Unit) {
            queryExecutor = FilteringApiQueryExecutor(
                dslContext = dslContext,
                conversionService = conversionService,
                root = root,
                init = spec,
                entityType = entityType
            )
        }

        internal fun createExecutor(): FilteringApiExecutor<E, DTO> {
            return FilteringApiExecutor(
                queryExecutor = queryExecutor,
                workspaceService = workspaceService,
                apiRequestResolver = apiRequestResolver,
                mapper = mapper
            )
        }
    }
}
