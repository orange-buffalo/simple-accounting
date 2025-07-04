package io.orangebuffalo.simpleaccounting.infra

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

/**
 * Central point for getting current time and date and working with them.
 *
 * Main purpose of this class is to support maintainable mocking of time-related operations
 * in the test code.
 */
@Service
class TimeService {
    fun currentTime(): Instant = Instant.now()
    fun currentDate(): LocalDate = LocalDate.now()
}
