package io.orangebuffalo.simpleaccounting.infra.persistence

import org.springframework.data.jdbc.core.mapping.JdbcMappingContext
import org.springframework.stereotype.Component

@Component
class EntityPropertyUpdater(
    private val jdbcMappingContext: JdbcMappingContext,
) {

    fun <T : Any> update(entity: T, propertyName: String, value: Any?): T {
        val persistentEntity = jdbcMappingContext.getRequiredPersistentEntity(entity.javaClass)
        val property = persistentEntity.getRequiredPersistentProperty(propertyName)
        val accessor = persistentEntity.getPropertyAccessor(entity)
        accessor.setProperty(property, value)

        @Suppress("UNCHECKED_CAST")
        return accessor.bean as T
    }
}
