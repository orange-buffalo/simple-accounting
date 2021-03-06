package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import org.jooq.DSLContext
import org.jooq.Table
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * @param E type of aggregate
 * @param DTO type of response DTO
 * @param SF type of enum that defines supported sorting fields
 * @param PR type of [ApiPageRequest] that describes and transports filtering query parameters
 */
class FilteringApiExecutor<E : Any, DTO : Any, SF : Enum<SF>, PR : ApiPageRequest<SF>>(
    private val queryExecutor: FilteringApiQueryExecutor<*, E, SF, PR>,
    private val workspaceService: WorkspaceService,
    private val mapper: suspend E.() -> DTO
) {
    suspend fun executeFiltering(
        request: PR,
        workspaceId: Long,
        mode: WorkspaceAccessMode = WorkspaceAccessMode.READ_ONLY
    ): ApiPage<DTO> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, mode)
        val entityPage = queryExecutor.executeFilteringQuery(request, workspace.id)
        return ApiPage(
            pageNumber = entityPage.pageNumber,
            pageSize = entityPage.pageSize,
            totalElements = entityPage.totalElements,
            data = entityPage.data.map { entity -> mapper(entity) }
        )
    }

    suspend fun executeFiltering(request: PR): ApiPage<DTO> {
        val entityPage = queryExecutor.executeFilteringQuery(request)
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
    private val workspaceService: WorkspaceService
) {
    final inline fun <reified E : Any, DTO : Any, SF : Enum<SF>, PR : ApiPageRequest<SF>> executor(
        noinline spec: ExecutorConfig<E, DTO, SF, PR>.() -> Unit
    ): FilteringApiExecutor<E, DTO, SF, PR> = executor(E::class, spec)

    fun <E : Any, DTO : Any, SF : Enum<SF>, PR : ApiPageRequest<SF>> executor(
        entityType: KClass<E>,
        spec: ExecutorConfig<E, DTO, SF, PR>.() -> Unit
    ): FilteringApiExecutor<E, DTO, SF, PR> {
        val config = ExecutorConfig<E, DTO, SF, PR>(entityType)
        spec(config)
        return config.createExecutor()
    }

    inner class ExecutorConfig<E : Any, DTO : Any, SF : Enum<SF>, PR : ApiPageRequest<SF>>(
        private val entityType: KClass<E>
    ) {
        private lateinit var queryExecutor: FilteringApiQueryExecutor<*, E, SF, PR>
        private lateinit var mapper: suspend E.() -> DTO

        fun mapper(spec: suspend E.() -> DTO) {
            this.mapper = spec
        }

        fun <T : Table<*>> query(root: T, spec: FilteringApiQuerySpec<T, SF, PR>.() -> Unit) {
            queryExecutor = FilteringApiQueryExecutor(
                dslContext = dslContext,
                root = root,
                init = spec,
                entityType = entityType
            )
        }

        internal fun createExecutor(): FilteringApiExecutor<E, DTO, SF, PR> {
            return FilteringApiExecutor(
                queryExecutor = queryExecutor,
                workspaceService = workspaceService,
                mapper = mapper
            )
        }
    }
}
