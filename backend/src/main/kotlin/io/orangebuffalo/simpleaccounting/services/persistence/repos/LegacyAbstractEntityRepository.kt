package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.LegacyAbstractEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
@Deprecated("Use AbstractEntityRepository")
interface LegacyAbstractEntityRepository<T : LegacyAbstractEntity> : JpaRepository<T, Long>
