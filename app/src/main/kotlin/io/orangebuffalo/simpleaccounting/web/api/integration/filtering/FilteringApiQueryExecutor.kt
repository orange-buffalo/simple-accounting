package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import io.orangebuffalo.simpleaccounting.infra.withDbContext
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import org.jooq.*
import org.jooq.impl.DSL
import kotlin.reflect.KClass

class FilteringApiQueryExecutor<T : Table<*>, E : Any, SF : Enum<SF>, PR : ApiPageRequest<SF>>(
    dslContext: DSLContext,
    root: T,
    entityType: KClass<E>,
    init: FilteringApiQuerySpec<T, SF, PR>.() -> Unit
) {

    private var querySpec: FilteringApiQuerySpecIml<T, E, SF, PR> =
        FilteringApiQuerySpecIml<T, E, SF, PR>(dslContext, root, entityType).apply(init)

    suspend fun executeFilteringQuery(request: PR, workspaceId: Long? = null): ApiPage<E> =
        querySpec.executeQuery(request, workspaceId)

    private class FilteringApiQuerySpecIml<T : Table<*>, E : Any, SF : Enum<SF>, PR : ApiPageRequest<SF>>(
        private val dslContext: DSLContext,
        override val root: T,
        private val entityType: KClass<E>
    ) : FilteringApiQuerySpec<T, SF, PR>, FilteringApiQuerySpec.HasRoot<T> {

        private val defaultSortingList: MutableList<FilteringApiQuerySpec.HasRoot<T>.() -> SortField<out Any>> =
            mutableListOf()
        private var workspaceFilter: (FilteringApiQuerySpec.WorkspaceFilterConfig<T>.(Long?) -> Condition)? = null
        private var queryConfigurer: (FilteringApiQuerySpec.QueryConfigurer<T>.() -> Unit)? = null
        private val filters: MutableList<Filters<PR, out Any?>> = mutableListOf()

        override fun workspaceFilter(spec: FilteringApiQuerySpec.WorkspaceFilterConfig<T>.(Long?) -> Condition) {
            workspaceFilter = spec
        }

        override fun configure(spec: FilteringApiQuerySpec.QueryConfigurer<T>.() -> Unit) {
            queryConfigurer = spec
        }

        override fun <V : Any?> onFilter(
            filterValueProvider: (PR) -> V,
            queryPredicateProvider: FilteringApiQuerySpec.HasRoot<T>.(predicateValue: V) -> Condition
        ) {
            filters.add(Filters(filterValueProvider, queryPredicateProvider))
        }

        override fun addDefaultSorting(init: FilteringApiQuerySpec.HasRoot<T>.() -> SortField<out Any>) {
            defaultSortingList.add(init)
        }

        suspend fun executeQuery(request: PR, workspaceId: Long?): ApiPage<E> = withDbContext {
            val countQuery = dslContext.selectCount().from(root)
            configureQuery(countQuery)
            val countPredicates: Collection<Condition> =
                getQueryPredicates(request, workspaceId, countQuery)
            val totalRecordsCount = countQuery.where(countPredicates).fetchOneInto(Long::class.java)!!

            val dataQuery = dslContext.select(*root.fields()).from(root)
            configureQuery(dataQuery)
            val dataPredicates: Collection<Condition> = getQueryPredicates(request, workspaceId, dataQuery)
            val pageNumber = request.pageNumber ?: 1
            val pageSize = request.pageSize ?: 10
            val data = dataQuery.where(dataPredicates)
                .orderBy(validateAndGetSorting())
                .limit((pageNumber - 1) * pageSize, pageSize)
                .fetchListOf(entityType)

            ApiPage(
                pageNumber = pageNumber,
                pageSize = pageSize,
                totalElements = totalRecordsCount,
                data = data
            )
        }

        private fun configureQuery(targetQuery: SelectJoinStep<out Record>) {
            val dataHolder = object : FilteringApiQuerySpec.QueryConfigurer<T> {
                override val query = targetQuery
                override val root = this@FilteringApiQuerySpecIml.root
            }
            queryConfigurer?.invoke(dataHolder)
        }

        private fun validateAndGetSorting(): List<SortField<out Any>> {
            return defaultSortingList.map { sorting -> sorting(this) }
        }

        private fun getQueryPredicates(
            request: PR,
            workspaceId: Long?,
            query: SelectJoinStep<out Record>
        ): Collection<Condition> = filters.asSequence()
            .map { filter -> filter.queryPredicate(request) }
            .plus(getWorkspaceFilterPredicate(workspaceId, query))
            .toList()

        private fun getWorkspaceFilterPredicate(workspaceId: Long?, query: SelectJoinStep<out Record>): Condition {
            if (workspaceId == null) {
                return DSL.trueCondition()
            }

            val filter = workspaceFilter
            check(filter != null) { "Workspace filter is required when workspace is provided" }
            val dataHolder = object : FilteringApiQuerySpec.WorkspaceFilterConfig<T> {
                override val query = query
                override val root = this@FilteringApiQuerySpecIml.root
            }
            return filter.invoke(dataHolder, workspaceId)
        }

        private inner class Filters<PR, V : Any?>(
            val valueProvider: (PR) -> V,
            val conditionProvider: FilteringApiQuerySpec.HasRoot<T>.(predicateValue: V) -> Condition
        ) {
            fun queryPredicate(pageRequest: PR): Condition {
                val filterValue = valueProvider.invoke(pageRequest) ?: return DSL.trueCondition()
                if (filterValue is Collection<*> && filterValue.isEmpty()) {
                    return DSL.trueCondition()
                }
                return conditionProvider.invoke(this@FilteringApiQuerySpecIml, filterValue)
            }
        }
    }
}

@DslMarker
annotation class FilteringApiDsl

@FilteringApiDsl
interface FilteringApiQuerySpec<T : Table<*>, SF : Enum<SF>, PR : ApiPageRequest<SF>> {

    fun configure(spec: QueryConfigurer<T>.() -> Unit)

    fun workspaceFilter(spec: WorkspaceFilterConfig<T>.(Long?) -> Condition)

    fun <V : Any?> onFilter(
        filterValueProvider: (PR) -> V,
        queryPredicateProvider: HasRoot<T>.(predicateValue: V) -> Condition
    )

    fun addDefaultSorting(init: HasRoot<T>.() -> SortField<out Any>)

    @FilteringApiDsl
    interface HasQuery {
        val query: SelectJoinStep<out Record>
    }

    @FilteringApiDsl
    interface HasRoot<T : Table<*>> {
        val root: T
    }

    @FilteringApiDsl
    interface WorkspaceFilterConfig<T : Table<*>> : HasRoot<T>, HasQuery

    @FilteringApiDsl
    interface QueryConfigurer<T : Table<*>> : HasRoot<T>, HasQuery
}

