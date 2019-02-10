package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import javax.persistence.*

@Entity
class Tax(

    @field:Column(nullable = false) var title: String,

    @field:Column(nullable = false) var rateInBps: Int,

    @field:Column(nullable = true) var description: String? = null,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "tax_workspace_fk"))
    val workspace: Workspace

) : AbstractEntity()