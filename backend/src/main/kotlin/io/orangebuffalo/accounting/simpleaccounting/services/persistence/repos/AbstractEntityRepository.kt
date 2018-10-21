package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface AbstractEntityRepository<T : AbstractEntity> : JpaRepository<T, Long>