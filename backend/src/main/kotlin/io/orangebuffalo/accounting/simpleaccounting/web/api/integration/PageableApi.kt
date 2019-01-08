package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PageableApi(val descriptorClass: KClass<out PageableApiDescriptor<*, *>>)

interface PageableApiDescriptor<E, R : EntityPath<E>> {
    suspend fun mapEntityToDto(entity: E): Any

    fun getSupportedFilters(): List<PageableApiFilter<E, R>> = emptyList()
}

enum class PageableApiFilterOperator(val apiValue: String) {
    EQ("eq"),
    GOE("goe"),
    LOE("loe");

    companion object {

        fun forApiValue(value: String): PageableApiFilterOperator? {
            return values().firstOrNull { it.apiValue == value }
        }
    }
}

class PageableApiFilter<E, R : EntityPath<E>>(
    val apiFieldName: String,
    private val root: R,
    private val predicateProviders: Map<PageableApiFilterOperator, PageableApiPredicateProvider<R, *>>
) {

    fun forOperator(operator: PageableApiFilterOperator, rawValue: String): Predicate {
        val provider = predicateProviders[operator]
            ?: throw ApiValidationException("'${operator.apiValue}' is not supported for '$apiFieldName'")
        return provider.predicateOnValue(root, rawValue)
    }

}

class PageableApiPredicateProvider<R, T>(
    val rawValueConverter: (String) -> T,
    val predicateBuilder: (R, T) -> Predicate
) {
    fun predicateOnValue(root: R, rawValue: String): Predicate {
        val entityFieldValue = convert(rawValue)
        return predicateBuilder(root, entityFieldValue)
    }

    private fun convert(rawValue: String): T {
        try {
            return rawValueConverter(rawValue)
        } catch (e: Exception) {
            throw throw ApiValidationException("'$rawValue' is not a valid filter value")
        }
    }
}
