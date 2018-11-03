package io.orangebuffalo.accounting.simpleaccounting.services.business

import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TimeService {

    fun currentTime(): Instant = Instant.now()

}