package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.intoListOf
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiValidationException
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiPredicateOperator.MultiArgumentsOperator
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiPredicateOperator.SingleArgumentOperator
import org.jooq.*
import org.springframework.core.convert.ConversionException
import org.springframework.core.convert.ConversionService
import kotlin.reflect.KClass

class FilteringApiQueryExecutor<T : Table<*>, E : Any>(
    dslContext: DSLContext,
    conversionService: ConversionService,
    root: T,
    entityType: KClass<E>,
    init: FilteringApiQuerySpec<T>.() -> Unit
) {

    private var querySpec: FilteringApiQuerySpecIml<T, E> =
        FilteringApiQuerySpecIml(dslContext, conversionService, root, entityType).apply(init)

    suspend fun executeFilteringQuery(request: FilteringApiRequest): ApiPage<E> = querySpec.executeQuery(request)

    private class FilteringApiQuerySpecIml<T : Table<*>, E : Any>(
        private val dslContext: DSLContext,
        private val conversionService: ConversionService,
        override val root: T,
        private val entityType: KClass<E>
    ) : FilteringApiQuerySpec<T>, FilteringApiQuerySpec.HasRoot<T> {

        private val conditionsSpecs: MutableMap<String, (SelectJoinStep<out Record>, FilteringApiRequestPredicate) -> Condition> =
            mutableMapOf()
        private val defaultSortingList: MutableList<FilteringApiQuerySpec.HasRoot<T>.() -> SortField<out Any>> =
            mutableListOf()

        override fun <V : Any> filterByField(
            apiFieldName: String,
            modelFieldType: KClass<V>,
            fieldsConditionsSpec: FilteringApiQuerySpec.FieldConditionsSpec<T, V>.() -> Unit
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

        override fun addDefaultSorting(init: FilteringApiQuerySpec.HasRoot<T>.() -> SortField<out Any>) {
            defaultSortingList.add(init)
        }

        suspend fun executeQuery(fileApiRequest: FilteringApiRequest): ApiPage<E> = withDbContext {
            val countQuery = dslContext.selectCount().from(root)
            val countConditions: Collection<Condition> = validateAndGetConditions(fileApiRequest, countQuery)
            val totalRecordsCount = countQuery.where(countConditions).fetchOneInto(Long::class.java)

            val dataQuery = dslContext.select().from(root)
            val dataConditions: Collection<Condition> = validateAndGetConditions(fileApiRequest, dataQuery)
            val data = dataQuery.where(dataConditions)
                .orderBy(validateAndGetSorting())
                .limit((fileApiRequest.pageNumber - 1) * fileApiRequest.pageSize, fileApiRequest.pageSize)
                .intoListOf(entityType)

            ApiPage(
                pageNumber = fileApiRequest.pageNumber,
                pageSize = fileApiRequest.pageSize,
                totalElements = totalRecordsCount,
                data = data
            )
        }

        private fun validateAndGetSorting(): List<SortField<out Any>> {
            return defaultSortingList.map { sorting -> sorting(this) }
        }

        private fun validateAndGetConditions(
            fileApiRequest: FilteringApiRequest,
            query: SelectJoinStep<out Record>
        ): Collection<Condition> = fileApiRequest.predicates
            .map { requestPredicate -> validatePredicateAndGetCondition(requestPredicate, query) }


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
        ) : FilteringApiQuerySpec.FieldConditionsSpec<T, V>,
            FilteringApiQuerySpec.HasRoot<T> by this@FilteringApiQuerySpecIml {

            private val conditionsSpecs: MutableMap<FilteringApiPredicateOperator, (String) -> Condition> =
                mutableMapOf()

            override fun onPredicate(
                operator: SingleArgumentOperator,
                init: FilteringApiQuerySpec.HasRoot<T>.(predicateValue: V) -> Condition
            ) {
                conditionsSpecs[operator] = { requestValue -> init(this, convertRequestValue(requestValue)) }
            }

            override fun onPredicate(
                operator: MultiArgumentsOperator,
                init: FilteringApiQuerySpec.HasRoot<T>.(predicateValue: Collection<V>) -> Condition
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
    }
}

@DslMarker
annotation class FilteringApiDsl

@FilteringApiDsl
interface FilteringApiQuerySpec<T : Table<*>> {

    fun <V : Any> filterByField(
        apiFieldName: String,
        modelFieldType: KClass<V>,
        fieldsConditionsSpec: FieldConditionsSpec<T, V>.() -> Unit
    )

    fun addDefaultSorting(init: HasRoot<T>.() -> SortField<out Any>)

    @FilteringApiDsl
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

    @FilteringApiDsl
    interface HasQuery {
        val query: SelectJoinStep<out Record>
    }

    @FilteringApiDsl
    interface HasRoot<T : Table<*>> {
        val root: T
    }
}

