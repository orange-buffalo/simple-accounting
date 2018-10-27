package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import javax.persistence.*

@Entity
class Category(

        @field:Column(nullable = false) var name: String,

        @field:Column(length = 1024) var description: String? = null,

        @field:ManyToOne(optional = false)
        @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "category_workspace_fk"))
        val workspace: Workspace,

        @field:Column(nullable = false) var income: Boolean,

        @field:Column(nullable = false) var expense: Boolean

) : AbstractEntity()