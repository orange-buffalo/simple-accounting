package io.orangebuffalo.simpleaccounting.services.persistence.entities

import org.springframework.data.relational.core.mapping.Table

@Table
class Customer(
    var name: String,
    val workspaceId: Long
) : AbstractEntity()
