package io.orangebuffalo.accounting.simpleaccounting.services.business

import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class TimeService {

    fun currentTime(): ZonedDateTime = ZonedDateTime.now()
}