package io.orangebuffalo.simpleaccounting.domain.categories

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.relational.core.mapping.Table

@Table
class Category(
    var name: String,
    var description: String? = null,
    val workspaceId: Long,
    var income: Boolean,
    var expense: Boolean
) : AbstractEntity()
