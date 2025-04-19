package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.kotest.matchers.bigdecimal.shouldHaveScale
import net.javacrumbs.jsonunit.kotest.shouldBeJsonNumber

fun Any?.shouldBeJsonInteger(): Int {
    return this.shouldBeJsonNumber()
        .shouldWithHint("Expected an integer") {
            this.shouldHaveScale(0)
        }
        .intValueExact()
}

object JsonValues {
    const val ANY_NUMBER: String = "#{json-unit.any-number}"
    const val ANY_STRING: String = "#{json-unit.any-string}"

    fun matchingBy(matcher: String) = "#{json-unit.matches:$matcher}"
}
