package io.orangebuffalo.simpleaccounting.services.persistence.entities

import javax.persistence.*

@Entity
class Workspace(

    @field:Column(nullable = false)
    var name: String,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "workspace_owner_fk"))
    val owner: PlatformUser,

    @field:Column(nullable = false)
    var taxEnabled: Boolean,

    @field:Column(nullable = false)
    var multiCurrencyEnabled: Boolean,

    @field:Column(nullable = false, length = 3)
    val defaultCurrency: String

) : LegacyAbstractEntity()
