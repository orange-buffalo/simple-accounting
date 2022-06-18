package io.orangebuffalo.simpleaccounting.utils

import mu.KotlinLogging

val logger = KotlinLogging.logger("storybook-tests")

class StopWatch {
    private val startTime = System.currentTimeMillis()
    private var lastLogTime = startTime

    fun tick(): Long {
        val newTime = System.currentTimeMillis()
        val result = newTime - lastLogTime
        lastLogTime = newTime
        return result
    }

    fun fromStart() = System.currentTimeMillis() - startTime
}
