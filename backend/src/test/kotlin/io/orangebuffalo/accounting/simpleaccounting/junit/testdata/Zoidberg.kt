package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData

class Zoidberg : TestData {

    val himself = Prototypes.zoidberg()

    override fun generateData() = listOf(himself)
}