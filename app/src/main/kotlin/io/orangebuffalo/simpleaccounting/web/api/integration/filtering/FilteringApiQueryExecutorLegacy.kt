package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.fetchListOf
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.ApiValidationException
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiPredicateOperator.MultiArgumentsOperator
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiPredicateOperator.SingleArgumentOperator
import org.jooq.*
import org.jooq.impl.DSL
import org.springframework.core.convert.ConversionException
import org.springframework.core.convert.ConversionService
import kotlin.reflect.KClass

class FilteringApiQueryExecutorLegacy<T : Table<*>, E : Any>(
    dslContext: DSLContext,
    conversionService: ConversionService,
    root: T,
    entityType: KClass<E>,
    init: FilteringApiQuerySpecLegacy<T>.() -> Unit
) {

    private var querySpec: FilteringApiQuerySpecIml<T, E> =
        FilteringApiQuerySpecIml(dslContext, conversionService, root, entityType).apply(init)

    suspend fun executeFilteringQuery(request: FilteringApiRequest, workspaceId: Long? = null): ApiPage<E> =
        querySpec.executeQuery(request, workspaceId)

    private class FilteringApiQuerySpecIml<T : Table<*>, E : Any>(
        private val dslContext: DSLContext,
        private val conversionService: ConversionService,
        override val root: T,
        private val entityType: KClass<E>
    ) : FilteringApiQuerySpecLegacy<T>, FilteringApiQuerySpecLegacy.HasRoot<T> {

        private val conditionsSpecs: MutableMap<String, (SelectJoinStep<out Record>, FilteringApiRequestPredicate) -> Condition> =
            mutableMapOf()
        private val defaultSortingList: MutableList<FilteringApiQuerySpecLegacy.HasRoot<T>.() -> SortField<out Any>> =
            mutableListOf()
        private var workspaceFilter: (FilteringApiQuerySpecLegacy.WorkspaceFilterConfig<T>.(Long?) -> Condition)? = null
        private var queryConfigurer: (FilteringApiQuerySpecLegacy.QueryConfigurer<T>.() -> Unit)? = null

        override fun <V : Any> filterByField(
            apiFieldName: String,
            modelFieldType: KClass<V>,
            fieldsConditionsSpec: FilteringApiQuerySpecLegacy.FieldConditionsSpec<T, V>.() -> Unit
        ) {
            conditionsSpecs[apiFieldName] = { query, requestPredicate ->
                val fieldConditions = FieldConditionsSpecImpl(
                    apiFieldName = requestPredicate.apiField,
                    valueType = modelFieldType,
                    query = query,
                    conversionService = conversionService
                )
                fieldsConditionsSpec(fieldConditions)
                fieldConditions.buildByOperator(requestPredicate.operator, requestPredicate.value)
            }
        }

        override fun addDefaultSorting(init: FilteringApiQuerySpecLegacy.HasRoot<T>.() -> SortField<out Any>) {
            defaultSortingList.add(init)
        }

        suspend fun executeQuery(fileApiRequest: FilteringApiRequest, workspaceId: Long?): ApiPage<E> = withDbContext {
            val countQuery = dslContext.selectCount().from(root)
            configureQuery(countQuery)
            val countConditions: Collection<Condition> =
                validateAndGetConditions(fileApiRequest, workspaceId, countQuery)
            val totalRecordsCount = countQuery.where(countConditions).fetchOneInto(Long::class.java)!!

            val dataQuery = dslContext.select(*root.fields()).from(root)
            configureQuery(dataQuery)
            val dataConditions: Collection<Condition> = validateAndGetConditions(fileApiRequest, workspaceId, dataQuery)
            val data = dataQuery.where(dataConditions)
                .orderBy(validateAndGetSorting())
                .limit((fileApiRequest.pageNumber - 1) * fileApiRequest.pageSize, fileApiRequest.pageSize)
                .fetchListOf(entityType)

            ApiPage(
                pageNumber = fileApiRequest.pageNumber,
                pageSize = fileApiRequest.pageSize,
                totalElements = totalRecordsCount,
                data = data
            )
        }

        private fun configureQuery(targetQuery: SelectJoinStep<out Record>) {
            val dataHolder = object : FilteringApiQuerySpecLegacy.QueryConfigurer<T> {
                override val query = targetQuery
                override val root = this@FilteringApiQuerySpecIml.root
            }
            queryConfigurer?.invoke(dataHolder)
        }

        private fun validateAndGetSorting(): List<SortField<out Any>> {
            return defaultSortingList.map { sorting -> sorting(this) }
        }

        private fun validateAndGetConditions(
            fileApiRequest: FilteringApiRequest,
            workspaceId: Long?,
            query: SelectJoinStep<out Record>
        ): Collection<Condition> = fileApiRequest.predicates
            .map { requestPredicate -> validatePredicateAndGetCondition(requestPredicate, query) }
            .plus(getWorkspaceFilterCondition(workspaceId, query))

        private fun getWorkspaceFilterCondition(workspaceId: Long?, query: SelectJoinStep<out Record>): Condition {
            if (workspaceId == null) {
                return DSL.trueCondition()
            }

            val filter = workspaceFilter
            check(filter != null) { "Workspace filter is required when workspace is provided" }
            val dataHolder = object : FilteringApiQuerySpecLegacy.WorkspaceFilterConfig<T> {
                override val query = query
                override val root = this@FilteringApiQuerySpecIml.root
            }
            return filter(dataHolder, workspaceId)
        }

        private fun validatePredicateAndGetCondition(
            requestPredicate: FilteringApiRequestPredicate,
            query: SelectJoinStep<out Record>
        ): Condition {
            val fieldConditionsSpec = conditionsSpecs[requestPredicate.apiField]
                ?: throw ApiValidationException("Filtering by '${requestPredicate.apiField}' is not supported")
            return fieldConditionsSpec(query, requestPredicate)
        }

        private inner class FieldConditionsSpecImpl<V : Any>(
            private val apiFieldName: String,
            private val valueType: KClass<V>,
            override val query: SelectJoinStep<out Record>,
            private val conversionService: ConversionService
        ) : FilteringApiQuerySpecLegacy.FieldConditionsSpec<T, V>,
            FilteringApiQuerySpecLegacy.HasRoot<T> by this@FilteringApiQuerySpecIml {

            private val conditionsSpecs: MutableMap<FilteringApiPredicateOperator, (String) -> Condition> =
                mutableMapOf()

            override fun onPredicate(
                operator: SingleArgumentOperator,
                init: FilteringApiQuerySpecLegacy.HasRoot<T>.(predicateValue: V) -> Condition
            ) {
                conditionsSpecs[operator] = { requestValue -> init(this, convertRequestValue(requestValue)) }
            }

            override fun onPredicate(
                operator: MultiArgumentsOperator,
                init: FilteringApiQuerySpecLegacy.HasRoot<T>.(predicateValue: Collection<V>) -> Condition
            ) {
                conditionsSpecs[operator] = { requestValue ->
                    init(this, requestValue.split(',').asSequence()
                        .map { singleValue -> convertRequestValue(singleValue) }
                        .toList()
                    )
                }
            }

            fun buildByOperator(operator: FilteringApiPredicateOperator, requestValue: String): Condition {
                val conditionSpec = conditionsSpecs[operator]
                    ?: throw ApiValidationException("'${operator.requestValue}' operator is not supported for '$apiFieldName' filter")
                return conditionSpec(requestValue)
            }

            private fun convertRequestValue(requestValue: String): V = try {
                conversionService.convert(requestValue, valueType.java)!!
            } catch (e: ConversionException) {
                throw ApiValidationException(
                    "Cannot convert '$requestValue' to ${valueType.simpleName} ('$apiFieldName')",
                    e
                )
            }
        }

        override fun workspaceFilter(spec: FilteringApiQuerySpecLegacy.WorkspaceFilterConfig<T>.(Long?) -> Condition) {
            workspaceFilter = spec
        }

        override fun configure(spec: FilteringApiQuerySpecLegacy.QueryConfigurer<T>.() -> Unit) {
            queryConfigurer = spec
        }
    }
}

@DslMarker
annotation class FilteringApiDslLegacy

@FilteringApiDslLegacy
interface FilteringApiQuerySpecLegacy<T : Table<*>> {

    fun configure(spec: QueryConfigurer<T>.() -> Unit)

    fun workspaceFilter(spec: WorkspaceFilterConfig<T>.(Long?) -> Condition)

    fun <V : Any> filterByField(
        apiFieldName: String,
        modelFieldType: KClass<V>,
        fieldsConditionsSpec: FieldConditionsSpec<T, V>.() -> Unit
    )

    fun addDefaultSorting(init: HasRoot<T>.() -> SortField<out Any>)

    @FilteringApiDslLegacy
    interface FieldConditionsSpec<T : Table<*>, V : Any> : HasRoot<T>, HasQuery {
        fun onPredicate(
            operator: SingleArgumentOperator,
            init: HasRoot<T>.(predicateValue: V) -> Condition
        )

        fun onPredicate(
            operator: MultiArgumentsOperator,
            init: HasRoot<T>.(predicateValue: Collection<V>) -> Condition
        )
    }

    @FilteringApiDslLegacy
    interface HasQuery {
        val query: SelectJoinStep<out Record>
    }

    @FilteringApiDslLegacy
    interface HasRoot<T : Table<*>> {
        val root: T
    }

    @FilteringApiDslLegacy
    interface WorkspaceFilterConfig<T : Table<*>> : HasRoot<T>, HasQuery

    @FilteringApiDslLegacy
    interface QueryConfigurer<T : Table<*>> : HasRoot<T>, HasQuery
}

