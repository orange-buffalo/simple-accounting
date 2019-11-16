package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import arrow.core.Either
import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Sort
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PageableApi(val descriptorClass: KClass<out PageableApiDescriptor<*, *>>)

interface PageableApiDescriptor<E, R : EntityPath<E>> {
    suspend fun mapEntityToDto(entity: E): Any

    fun getSupportedFilters(): List<PageableApiFilter<E, R>> = emptyList()

    fun getDefaultSorting(): Sort = Sort.by(Sort.Order.desc("id"))

    fun getSupportedSorting(): Map<String, String> = emptyMap()
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

    fun forOperator(operator: PageableApiFilterOperator, rawValue: String): Either<String, Predicate> {
        val provider = predicateProviders[operator]
            ?: return Either.left("'${operator.apiValue}' is not supported for '$apiFieldName'")
        return provider.predicateOnValue(root, rawValue)
    }

}

class PageableApiPredicateProvider<R, T>(
    val rawValueConverter: (String) -> T,
    val predicateBuilder: (R, T) -> Predicate
) {
    fun predicateOnValue(root: R, rawValue: String): Either<String, Predicate> =
        convert(rawValue)
            .map { entityFieldValue -> predicateBuilder(root, entityFieldValue) }

    private fun convert(rawValue: String): Either<String, T> = try {
        Either.right(rawValueConverter(rawValue))
    } catch (e: Exception) {
        Either.left("'$rawValue' is not a valid filter value")
    }
}
