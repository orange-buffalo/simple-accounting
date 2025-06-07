package io.orangebuffalo.simpleaccounting.tests.infra.utils

import mu.KotlinLogging

val logger = KotlinLogging.logger("tests")

class StopWatch {
    private val startTime = System.currentTimeMillis()
    private var lastLogTime = startTime
    private val logBuilder = StringBuilder()

    fun tick(marker: String? = null): Long {
        val newTime = System.currentTimeMillis()
        val result = newTime - lastLogTime
        lastLogTime = newTime
        if (marker != null) {
            if (logBuilder.isNotEmpty()) {
                logBuilder.append(" ")
            }
            logBuilder.append("[$marker: ${result}ms]")
        }
        return result
    }

    fun fromStart() = System.currentTimeMillis() - startTime

    fun log(): String = logBuilder.toString()
}
