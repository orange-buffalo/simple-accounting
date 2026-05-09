package io.orangebuffalo.simpleaccounting.infra.persistence

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import io.orangebuffalo.simpleaccounting.infra.TimeService
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback
import org.springframework.stereotype.Component

@Component
class CreatedAtPopulatingCallback(
    private val timeService: TimeService,
    private val entityPropertyUpdater: EntityPropertyUpdater,
) : BeforeConvertCallback<AbstractEntity> {

    override fun onBeforeConvert(entity: AbstractEntity): AbstractEntity {
        return if (entity.version == null && entity.createdAt == null) {
            entityPropertyUpdater.update(entity, AbstractEntity::createdAt.name, timeService.currentTime())
        } else entity
    }
}
