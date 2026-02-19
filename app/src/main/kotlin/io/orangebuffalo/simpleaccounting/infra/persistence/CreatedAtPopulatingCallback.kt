package io.orangebuffalo.simpleaccounting.infra.persistence

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import io.orangebuffalo.simpleaccounting.infra.TimeService
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback
import org.springframework.stereotype.Component

@Component
class CreatedAtPopulatingCallback(
    private val timeService: TimeService
) : BeforeConvertCallback<AbstractEntity> {

    override fun onBeforeConvert(entity: AbstractEntity): AbstractEntity {
        if (entity.id == null && entity.createdAt == null) {
            entity.createdAt = timeService.currentTime()
        }
        return entity
    }
}
