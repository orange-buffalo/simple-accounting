package io.orangebuffalo.simpleaccounting.business.common.pesistence

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface AbstractEntityRepository<T : AbstractEntity> : CrudRepository<T, Long>
