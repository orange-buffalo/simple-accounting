package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import javax.persistence.*

@Entity
class Customer(

    @field:Column(nullable = false) var name: String,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "customer_workspace_fk"))
    val workspace: Workspace

) : AbstractEntity()