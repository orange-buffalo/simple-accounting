package io.orangebuffalo.simpleaccounting.infra.utils

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

val MOCK_TIME: Instant = ZonedDateTime.of(1999, 3, 28, 18, 1, 2, 42000000, ZoneId.of("America/New_York")).toInstant()
const val MOCK_TIME_VALUE = "1999-03-28T23:01:02.042Z"

fun mockCurrentTime(timeService: TimeService) {
    whenever(timeService.currentTime()) doReturn MOCK_TIME
}

fun mockCurrentDate(timeService: TimeService) {
    whenever(timeService.currentDate()) doReturn MOCK_DATE
}

val MOCK_DATE: LocalDate = LocalDate.of(1999, 3, 28)
const val MOCK_DATE_VALUE = "1999-03-28"

