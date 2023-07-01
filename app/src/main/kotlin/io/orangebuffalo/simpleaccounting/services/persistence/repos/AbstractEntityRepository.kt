package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface AbstractEntityRepository<T : AbstractEntity> : CrudRepository<T, Long>
