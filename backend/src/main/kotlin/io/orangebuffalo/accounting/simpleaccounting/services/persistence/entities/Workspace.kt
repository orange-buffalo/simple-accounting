package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class Workspace(
        @field:Column(nullable = false) var name: String,
        @field:ManyToOne(optional = false) @field:JoinColumn(nullable = false) val owner: PlatformUser,
        @field:Column(nullable = false) var taxEnabled: Boolean,
        @field:Column(nullable = false) var multiCurrencyEnabled: Boolean,
        @field:Column(nullable = false) val defaultCurrency: String
) : AbstractEntity()