package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.kotest.matchers.bigdecimal.shouldHaveScale
import net.javacrumbs.jsonunit.kotest.shouldBeJsonNumber

fun Any?.shouldBeJsonInteger(): Int {
    return this.shouldBeJsonNumber()
        .shouldWithClue("Expected an integer") {
            this.shouldHaveScale(0)
        }
        .intValueExact()
}
