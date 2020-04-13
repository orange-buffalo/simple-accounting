package io.orangebuffalo.simpleaccounting.services.persistence

import org.jooq.Configuration
import org.jooq.Result
import org.jooq.ResultQuery
import org.springframework.data.jdbc.core.convert.EntityRowMapper
import org.springframework.data.jdbc.core.convert.JdbcConverter
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity
import kotlin.reflect.KClass

private val jdbcConverterKey: KClass<JdbcConverter> = JdbcConverter::class
private val jdbcMappingContextKey: KClass<JdbcMappingContext> = JdbcMappingContext::class

fun Configuration.set(jdbcConverter: JdbcConverter): Configuration {
    data(jdbcConverterKey, jdbcConverter)
    return this
}

fun Configuration.set(jdbcMappingContext: JdbcMappingContext): Configuration {
    data(jdbcMappingContextKey, jdbcMappingContext)
    return this
}

fun <T : Any> Result<*>.intoListOf(targetEntityType: KClass<T>): List<T> {
    val jdbcConverter = this.configuration().data(jdbcConverterKey) as JdbcConverter
    val jdbcMappingContext = this.configuration().data(jdbcMappingContextKey) as JdbcMappingContext
    val persistentEntity = jdbcMappingContext.getPersistentEntity(targetEntityType.java)
        ?: throw IllegalStateException("$targetEntityType is not a known entity type")

    @Suppress("UNCHECKED_CAST")
    val mapper = EntityRowMapper<T>(persistentEntity as RelationalPersistentEntity<T>, jdbcConverter)

    val resultSet = this.intoResultSet()
    var rowNumber = 1
    val result = mutableListOf<T>()
    while (resultSet.next()) {
        result.add(mapper.mapRow(resultSet, rowNumber++))
    }

    return result
}

inline fun <reified T : Any> Result<*>.intoListOf(): List<T> = intoListOf(T::class)

inline fun <reified T : Any> ResultQuery<*>.intoListOf(): List<T> = fetch().intoListOf()

fun <T : Any> ResultQuery<*>.intoListOf(targetEntityType: KClass<T>): List<T> = fetch().intoListOf(targetEntityType)
