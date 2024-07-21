package io.orangebuffalo.simpleaccounting.domain.customers

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.relational.core.mapping.Table

@Table
class Customer(
    var name: String,
    val workspaceId: Long
) : AbstractEntity()
