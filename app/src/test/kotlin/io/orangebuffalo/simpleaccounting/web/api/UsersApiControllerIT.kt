package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData

class UserApiTestData : TestData {
    val farnsworth = Prototypes.farnsworth()
    val fry = Prototypes.fry()
    val zoidberg = Prototypes.platformUser(
        userName = "Zoidberg",
        isAdmin = false,
        activated = false
    )

    override fun generateData() = listOf(farnsworth, fry, zoidberg)
}
