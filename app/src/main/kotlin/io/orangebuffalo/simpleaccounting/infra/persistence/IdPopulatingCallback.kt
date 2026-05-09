package io.orangebuffalo.simpleaccounting.infra.persistence

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback
import org.springframework.stereotype.Component

private const val ENTITY_ID_LENGTH = 10

@Component
class IdPopulatingCallback(
    private val tokenGenerator: TokenGenerator,
    private val entityPropertyUpdater: EntityPropertyUpdater,
) : BeforeConvertCallback<AbstractEntity> {

    override fun onBeforeConvert(entity: AbstractEntity): AbstractEntity {
        return if (entity.id == null) {
            entityPropertyUpdater.update(entity, AbstractEntity::id.name, tokenGenerator.generateToken(ENTITY_ID_LENGTH))
        } else entity
    }
}
