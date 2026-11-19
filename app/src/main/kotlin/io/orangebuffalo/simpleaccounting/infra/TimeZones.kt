package io.orangebuffalo.simpleaccounting.infra

import java.time.ZoneId

// JVM-only aliases are not portable to browsers and PostgreSQL.
private val supportedTimeZones = ZoneId.getAvailableZoneIds().filterTo(destination = mutableSetOf("UTC")) {
    '/' in it && !it.startsWith("SystemV/")
}

fun isSupportedTimeZone(timeZone: String): Boolean = timeZone in supportedTimeZones
