package io.orangebuffalo.simpleaccounting.services.persistence.entities

import javax.persistence.*

/**
 * [General tax](https://en.wikipedia.org/wiki/List_of_taxes) includes taxes that
 * are typically added into the income / expense amounts, for example Value Added Tax,
 * Sales Tax, etc. They are processed separately and not included into the income tax.
 */
@Entity
class GeneralTax(

    @field:Column(nullable = false) var title: String,

    @field:Column(nullable = false) var rateInBps: Int,

    @field:Column(nullable = true) var description: String? = null,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "general_tax_workspace_fk"))
    val workspace: Workspace

) : AbstractEntity()
