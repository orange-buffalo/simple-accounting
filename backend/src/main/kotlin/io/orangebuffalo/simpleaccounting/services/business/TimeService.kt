package io.orangebuffalo.simpleaccounting.services.business

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class TimeService {

    fun currentTime(): Instant = Instant.now()
    fun currentDate(): LocalDate = LocalDate.now()
}
