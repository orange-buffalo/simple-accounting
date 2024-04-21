package io.orangebuffalo.simpleaccounting.infra.utils

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import org.springframework.data.jdbc.core.JdbcAggregateTemplate

/**
 * Finds a single entity of the specified type. Fails if there are no entities or more than one entity.
 */
inline fun <reified T : Any> JdbcAggregateTemplate.findSingle(): T =
    withClue("Exactly one entity of ${T::class} is expected") {
        this
            .findAll(T::class.java)
            .shouldHaveSize(1)
            .single()
    }
