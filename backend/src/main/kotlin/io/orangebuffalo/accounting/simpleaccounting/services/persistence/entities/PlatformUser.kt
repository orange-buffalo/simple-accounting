package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import javax.persistence.Column
import javax.persistence.Entity

@Entity
class PlatformUser(
        @field:Column(nullable = false) var userName: String,
        @field:Column(nullable = false) var passwordHash: String,
        @field:Column(nullable = false) var isAdmin: Boolean
) : AbstractEntity() {

}

