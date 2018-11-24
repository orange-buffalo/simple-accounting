package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Expression
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.ComparableExpressionBase
import com.querydsl.core.types.dsl.NumberExpression
import kotlin.reflect.KClass

fun <E, R : EntityPath<E>> apiFilters(
    root: R,
    spec: PageableApiFiltersBuilder<E, R>.() -> Unit
): List<PageableApiFilter<E, R>> = PageableApiFiltersBuilder(root).also(spec).build()


class PageableApiFiltersBuilder<E, R : EntityPath<E>>(
    private val root: R
) {
    private val filters: MutableList<PageableApiFilter<E, R>> = mutableListOf()


    fun <T : Any> byApiField(apiFieldName: String, fieldType: KClass<T>, spec: PageableApiFilterBuilder<T>.() -> Unit) {
        val filterBuilder = PageableApiFilterBuilder(apiFieldName, getConverter(fieldType))
        filterBuilder.spec()
        filters.add(filterBuilder.build())
    }

    fun <T : Any> mapApiFieldToEntityPath(apiFieldName: String, fieldType: KClass<T>, spec: R.() -> Expression<T>) {
        byApiField(apiFieldName, fieldType) {
            applyOperatorsTo(spec)
        }
    }

    private fun <T : Any> getConverter(fieldType: KClass<T>): (String) -> T {
        return when (fieldType) {
            Long::class -> { rawValue -> rawValue.toLong() as T }
            String::class -> { rawValue -> rawValue as T }
            else -> throw IllegalArgumentException("$fieldType is not supported")
        }
    }

    internal fun build() = filters

    inner class PageableApiFilterBuilder<T>(
        private val apiFieldName: String,
        private var rawValueConverter: (String) -> T
    ) {
        private val predicateProviders = mutableMapOf<PageableApiFilterOperator, PageableApiPredicateProvider<R, T>>()

        fun applyOperatorsTo(spec: R.() -> Expression<T>) {
            val filteredPath = root.spec()

            if (filteredPath is ComparableExpressionBase) {
                onOperator(PageableApiFilterOperator.EQ) { value -> filteredPath.eq(value) }
            }

            if (filteredPath is NumberExpression) {
                onOperator(PageableApiFilterOperator.GOE) { value ->
                    if (value is Long) {
                        filteredPath.goe(value as Long)
                    } else {
                        throw IllegalStateException("Unsupported type")
                    }
                }

                onOperator(PageableApiFilterOperator.LOE) { value ->
                    if (value is Long) {
                        filteredPath.loe(value as Long)
                    } else {
                        throw IllegalStateException("Unsupported type")
                    }
                }
            }
        }

        fun onOperator(operator: PageableApiFilterOperator, spec: R.(value: T) -> Predicate) {
            predicateProviders[operator] = PageableApiPredicateProvider(
                rawValueConverter,
                { entityPath, value -> entityPath.spec(value) }
            )
        }

        internal fun build(): PageableApiFilter<E, R> {
            return PageableApiFilter(
                apiFieldName = apiFieldName,
                root = root,
                predicateProviders = predicateProviders
            )
        }
    }
}
