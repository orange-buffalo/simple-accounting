package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import kotlin.reflect.KProperty

/**
 * Finds a single entity of the specified type. Fails if there are no entities or more than one entity.
 */
inline fun <reified T : Any> JdbcAggregateTemplate.findSingle(): T =
    withHint("Exactly one entity of ${T::class} is expected") {
        this.findAll(T::class.java).shouldBeSingle()
    }

/**
 * Finds a single entity of the specified type by ID. Fails if there are no entities found.
 */
inline fun <reified T : Any> JdbcAggregateTemplate.findSingle(id: Long): T =
    withHint("Exactly one entity of ${T::class} is expected for ID $id") {
        this.findById(id, T::class.java).shouldNotBeNull()
    }

/**
 * Loads all entities of the specified type.
 */
inline fun <reified T : Any> JdbcAggregateTemplate.findAll(): List<T> = this.findAll(T::class.java).toList()

/**
 * Verifies that the specified entity is deeply equal to the provided entity, ignoring generated entity fields.
 */
fun <T : AbstractEntity> T.shouldBeEntityWithFields(entity: T, vararg ignoredProperties: KProperty<*>) {
    this.shouldBeEqualToIgnoringFields(
        other = entity, AbstractEntity::id, AbstractEntity::version, AbstractEntity::createdAt, *ignoredProperties
    )
}
