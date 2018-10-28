package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser

class Prototypes {
    companion object {
        fun fry() = PlatformUser(
            userName = "Fry",
            passwordHash = "qwertyHash",
            isAdmin = false
        )

        fun farnsworth() = PlatformUser(
            userName = "Farnsworth",
            passwordHash = "scienceBasedHash",
            isAdmin = true
        )

        fun zoidberg() = PlatformUser(
            userName = "Zoidberg",
            passwordHash = "??",
            isAdmin = false
        )
    }
}